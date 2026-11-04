package com.positionbook.service.impl;

import com.positionbook.enums.EventType;
import com.positionbook.exception.ApplicationException;
import com.positionbook.exception.ResourceNotFoundException;
import com.positionbook.exception.TradeProcessingException;
import com.positionbook.mapper.PositionKeyMapper;
import com.positionbook.model.*;
import com.positionbook.service.IPositionBookService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies trade events to an in-memory position book.
 *
 * <p>Each batch is staged before the current immutable state is replaced, so a
 * failure in any account/security group leaves the complete state unchanged.
 * The synchronized batch entry point serializes state publication while work
 * for independent position groups runs concurrently.</p>
 */
@Service
public class PositionBookService implements IPositionBookService {

    private final PositionKeyMapper positionKeyMapperMapper;

    private volatile State state = new State(
            Map.of(),
            Map.of(),
            Map.of()
    );

    /**
     * Creates the service with the mapper used to derive position keys.
     *
     * @param positionKeyMapperMapper maps events and identifiers to position keys
     */
    public PositionBookService(PositionKeyMapper positionKeyMapperMapper) {
        this.positionKeyMapperMapper = positionKeyMapperMapper;
    }

    /**
     * Validates, stages, and atomically publishes a batch of trade events.
     *
     * <p>Events for each account/security pair are processed in event-ID order;
     * independent pairs are evaluated concurrently. The batch is committed only
     * after every group succeeds.</p>
     *
     * @param tradeEvents events to apply
     * @return updated positions in their first-seen group order
     * @throws IllegalArgumentException if the batch contains duplicate original IDs or duplicate cancellations
     * @throws TradeProcessingException if any group fails validation or processing
     */
    @Override
    public synchronized List<Position> processEvents(List<TradeEvent> tradeEvents) {

        // Read the current state.
        State currentState = state;

        // A cancellation intentionally reuses its original trade's event ID.
        Set<Long> originalEventIds = new HashSet<>();
        Set<Long> cancellationEventIds = new HashSet<>();
        Map<Long, TradeEvent> batchOriginalTrades = new HashMap<>();
        for (TradeEvent event : tradeEvents) {
            if (event.eventType() == EventType.CANCEL) {
                if (!cancellationEventIds.add(event.eventId())) {
                    throw new IllegalArgumentException("Multiple cancellations with the same ID found in the request.");
                }
            } else {
                if (!originalEventIds.add(event.eventId())) {
                    throw new IllegalArgumentException("Multiple original trades with the same ID found in the request.");
                }
                batchOriginalTrades.put(event.eventId(), event);
            }
        }

        Map<PositionKey, List<TradeEvent>> groupedEvents = new LinkedHashMap<>();

        tradeEvents.stream()
                .sorted(Comparator.comparingLong(TradeEvent::eventId))
                .forEach(event -> {

                    TradeEvent originalTrade = event.eventType() == EventType.CANCEL
                            ? batchOriginalTrades.getOrDefault(event.eventId(),
                                    currentState.tradeHistory().get(event.eventId()))
                            : event;
                    PositionKey key = originalTrade == null
                            ? positionKeyMapperMapper.toModel(event)
                            : positionKeyMapperMapper.toModel(originalTrade);
                    groupedEvents.computeIfAbsent(
                            key, k -> new ArrayList<>()
                    ).add(event);
                });


        // Each thread produces its own result.
        Map<PositionKey, GroupResult> success = new ConcurrentHashMap<>();
        Map<PositionKey, RuntimeException> error = new ConcurrentHashMap<>();
        List<Thread> threads = new ArrayList<>();

        // Process independent groups in parallel.
        for (Map.Entry<PositionKey, List<TradeEvent>> entry : groupedEvents.entrySet()) {

            PositionKey key = entry.getKey();
            List<TradeEvent> events = entry.getValue();

            Thread thread = Thread.startVirtualThread(() -> {

                try {
                    GroupResult result = processGroup(key, events, currentState);
                    success.put(key, result);
                } catch (RuntimeException exception) {
                    error.put(key, exception);
                }
            });

            threads.add(thread);
        }

        // Wait until all virtual threads finish.
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ApplicationException("Trade Event processing was interrupted", exception);
            }
        }

        // Collect every group failure so clients receive all batch errors at once.
        List<String> errors = new ArrayList<>();
        List<RuntimeException> processingExceptions = new ArrayList<>();

        for (PositionKey key : groupedEvents.keySet()) {

            RuntimeException exception = error.get(key);

            if (exception != null) {
                errors.add(exception.getMessage());
                processingExceptions.add(exception);
            }
        }

        if (!errors.isEmpty()) {
            throw new TradeProcessingException(errors, processingExceptions);
        }

        // Prepare the next state without modifying the current state.
        Map<PositionKey, Position> nextPositions = new ConcurrentHashMap<>(currentState.positions());
        Map<Long, TradeEvent> nextHistoryTrades = new ConcurrentHashMap<>(currentState.tradeHistory());
        Map<Long, TradeEvent> nextCancelledTrades = new ConcurrentHashMap<>(currentState.tradeCancelled());

        // Merge all successful group results.
        for (Map.Entry<PositionKey, GroupResult> entry : success.entrySet()) {

            GroupResult result = entry.getValue();
            nextPositions.put(entry.getKey(), result.position());
            nextHistoryTrades.putAll(result.originalTrades());
            nextCancelledTrades.putAll(result.cancelledTrades());
        }

        // Publish everything together.
        state = new State(
                Map.copyOf(nextPositions),
                Map.copyOf(nextHistoryTrades),
                Map.copyOf(nextCancelledTrades)
        );

        // Return updated positions.
        List<Position> response = new ArrayList<>();

        for (PositionKey key : groupedEvents.keySet()) {
            response.add(state.positions().get(key));
        }

        return response;
    }

    /**
     * Returns the stored position for an account/security pair.
     *
     * @param accountId account identifier
     * @param securityId security identifier
     * @return stored position
     */
    @Override
    public Position getPosition(String accountId, String securityId) {

        PositionKey key = positionKeyMapperMapper.toModel(accountId, securityId);
        Position position = state.positions().get(key);

        if (position == null) {
            throw new ResourceNotFoundException(
                    "Position not found for account " + accountId + " and security " + securityId);
        }

        return position;
    }


    /**
     * Processes one position group's events against the same starting snapshot.
     *
     * @param key position key for this group
     * @param events events for the group, already sorted by event ID
     * @param currentState state snapshot at the start of the batch
     * @return staged position and trade-history changes
     */
    private GroupResult processGroup(PositionKey key, List<TradeEvent> events, State currentState) {

        Position currentPosition = currentState.positions().get(key);
        Map<Long, TradeEvent> newHistoryTrade = new ConcurrentHashMap<>();
        Map<Long, TradeEvent> newCancelledTrade = new ConcurrentHashMap<>();

        for (TradeEvent event : events) {

            switch (event.eventType()) {

                case BUY -> currentPosition = processBuy(event, key, currentPosition, newHistoryTrade, currentState);
                case SELL -> currentPosition = processSell(event, key, currentPosition, newHistoryTrade, currentState);
                case CANCEL -> currentPosition = processCancellation(event, key, currentPosition, newHistoryTrade,
                            newCancelledTrade, currentState);
                default -> throw new IllegalArgumentException("Unsupported trade event type" + event.eventType()
                        + " for event id: " + event.eventId());
            }
        }

        return new GroupResult(currentPosition, newHistoryTrade, newCancelledTrade);
    }

    /**
     * Validates and stages a purchase, increasing the net position.
     *
     * @param tradeEvent purchase event
     * @param positionKey affected position key
     * @param currentPosition position accumulated so far in this group
     * @param newHistoryTrade staged trade-history changes
     * @param currentState snapshot used for duplicate-ID checks
     * @return updated position
     */
    private Position processBuy(TradeEvent tradeEvent, PositionKey positionKey, Position currentPosition,
                                Map<Long, TradeEvent> newHistoryTrade, State currentState) {

        validateQuantity(tradeEvent);
        validateEventIdUniqueness(tradeEvent, currentState);
        registerTrade(tradeEvent, newHistoryTrade);
        return updatePosition(positionKey, currentPosition, tradeEvent.quantity(), tradeEvent);
    }

    /**
     * Validates and stages a sale, requiring sufficient available quantity.
     *
     * @param tradeEvent sale event
     * @param positionKey affected position key
     * @param currentPosition position accumulated so far in this group
     * @param newHistoryTrade staged trade-history changes
     * @param currentState snapshot used for duplicate-ID checks
     * @return updated position
     */
    private Position processSell(TradeEvent tradeEvent, PositionKey positionKey, Position currentPosition,
                                 Map<Long, TradeEvent> newHistoryTrade, State currentState) {

        validateQuantity(tradeEvent);
        validateEventIdUniqueness(tradeEvent, currentState);
        registerTrade(tradeEvent, newHistoryTrade);

        return updatePosition(positionKey, currentPosition, -tradeEvent.quantity(), tradeEvent);
    }

    /**
     * Reverses the original trade and stages the cancellation record.
     *
     * @param event cancellation event referencing the original trade ID
     * @param positionKey affected position key
     * @param currentPosition position accumulated so far in this group
     * @param newHistoryTrade newly staged original trades, including earlier events in this batch
     * @param newCancelledTrade staged cancellation records
     * @param currentState snapshot containing previously accepted trades/cancellations
     * @return updated position after reversing the original quantity
     */
    private Position processCancellation(TradeEvent event, PositionKey positionKey, Position currentPosition,
                                         Map<Long, TradeEvent> newHistoryTrade, Map<Long, TradeEvent> newCancelledTrade,
                                         State currentState) {

        TradeEvent originalTrade = newHistoryTrade.get(event.eventId());

        if (originalTrade == null) {
            originalTrade = currentState.tradeHistory().get(event.eventId());
        }

        if (originalTrade == null) {
            throw new ResourceNotFoundException((
                    "Trade event not found for cancellation. Event Id: " + event.eventId())
            );
        }

        // Check whether the trade was already canceled.
        if (currentState.tradeCancelled().containsKey(event.eventId())) {
            throw new IllegalStateException("The Trade Event has already been cancelled. Event Id: " + event.eventId());
        }

        long reversalQuantity = switch (originalTrade.eventType()) {
            case BUY -> -originalTrade.quantity();
            case SELL -> originalTrade.quantity();
            case CANCEL -> throw new IllegalStateException("Cannot cancel a cancellation event. Event Id: " + event.eventId());
        };

        newCancelledTrade.put(event.eventId(), event);
        return updatePosition(positionKey, currentPosition, reversalQuantity, event);
    }

    /**
     * Applies a signed quantity change and appends the event to the position history.
     *
     * @param key affected position key
     * @param currentPosition position accumulated so far, or {@code null} if absent
     * @param quantityChange signed quantity delta
     * @param event event to append
     * @return a new position with copied event history
     * @throws ArithmeticException if the resulting quantity overflows a {@code long}
     */
    private Position updatePosition(PositionKey key, Position currentPosition,
                                    long quantityChange, TradeEvent event) {

        long currentQuantity = currentPosition == null ? 0L : currentPosition.quantity();
        long updatedQuantity = Math.addExact(currentQuantity, quantityChange);

        List<TradeEvent> events = new ArrayList<>();

        if (currentPosition != null) {
            events.addAll(currentPosition.events());
        }

        events.add(event);
        return new Position(key.accountId(), key.securityId(), updatedQuantity, List.copyOf(events));
    }

    /**
     * Adds an original trade to the batch's staged history.
     *
     * @param event trade to register
     * @param newTradeEvent staged trade-history map
     */
    private void registerTrade(TradeEvent event, Map<Long, TradeEvent> newTradeEvent) {
        newTradeEvent.put(event.eventId(), event);
    }

    /**
     * Rejects event IDs that already belong to an accepted original trade.
     *
     * @param event event being validated
     * @param currentState state snapshot at the start of the batch
     * @throws IllegalStateException if the ID has already been processed
     */
    private void validateEventIdUniqueness(TradeEvent event, State currentState) {
        if (currentState.tradeHistory().containsKey(event.eventId())) {
            throw new IllegalStateException("Trade event has already been processed. Event Id: " + event.eventId());
        }
    }

    /**
     * Ensures a BUY or SELL quantity is positive.
     *
     * @param event trade event to validate
     * @throws IllegalArgumentException if the quantity is zero or negative
     */
    private void validateQuantity(TradeEvent event) {
        if (event.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero for BUY and SELL. Event Id: " + event.eventId());
        }
    }

}
