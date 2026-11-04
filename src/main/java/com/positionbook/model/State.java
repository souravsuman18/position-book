package com.positionbook.model;

import java.util.Map;

/**
 * Immutable snapshot of the in-memory position book.
 *
 * @param positions current positions keyed by account/security
 * @param tradeHistory accepted original BUY/SELL events keyed by event ID
 * @param tradeCancelled cancellation events keyed by original event ID
 */
public record State(
        Map<PositionKey, Position> positions,
        Map<Long, TradeEvent> tradeHistory,
        Map<Long, TradeEvent> tradeCancelled
) {
    public State {
        positions = Map.copyOf(positions);
        tradeHistory = Map.copyOf(tradeHistory);
        tradeCancelled = Map.copyOf(tradeCancelled);
    }
}
