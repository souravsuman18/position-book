package com.positionbook.service.impl;

import com.positionbook.enums.EventType;
import com.positionbook.exception.ResourceNotFoundException;
import com.positionbook.exception.TradeProcessingException;
import com.positionbook.mapper.PositionKeyMapper;
import com.positionbook.model.Position;
import com.positionbook.model.TradeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionBookServiceTest {

    private PositionBookService service;

    @BeforeEach
    void setUp() {
        service = new PositionBookService(new PositionKeyMapper());
    }

    @Test
    void processesBuyAndSellEventsForSeparatePositions() {
        service.processEvents(List.of(
                event(3, EventType.BUY, "ACC1", "SEC1", 12),
                event(4, EventType.BUY, "ACC1", "SECXYZ", 50),
                event(5, EventType.BUY, "ACC2", "SECXYZ", 33),
                event(6, EventType.BUY, "ACC1", "SEC1", 20),
                event(10, EventType.BUY, "ACC3", "SEC1", 100),
                event(11, EventType.SELL, "ACC3", "SEC1", 50)
        ));

        assertEquals(32, service.getPosition("ACC1", "SEC1").quantity());
        assertEquals(50, service.getPosition("ACC1", "SECXYZ").quantity());
        assertEquals(33, service.getPosition("ACC2", "SECXYZ").quantity());
        assertEquals(50, service.getPosition("ACC3", "SEC1").quantity());
        assertEquals(2, service.getPosition("ACC3", "SEC1").events().size());
    }

    @Test
    void cancellationInSameBatchReversesOriginalTradeAndIgnoresItsSecurityAndQuantity() {
        List<Position> positions = service.processEvents(List.of(
                event(21, EventType.BUY, "ACC1", "SEC1", 100),
                event(21, EventType.CANCEL, "ACC1", "IGNORED_SECURITY", 0),
                event(22, EventType.BUY, "ACC1", "SEC1", 5)
        ));

        assertEquals(1, positions.size());
        Position position = service.getPosition("ACC1", "SEC1");
        assertEquals(5, position.quantity());
        assertEquals(List.of(EventType.BUY, EventType.CANCEL, EventType.BUY),
                position.events().stream().map(TradeEvent::eventType).toList());
        assertEquals(List.of(21L, 21L, 22L),
                position.events().stream().map(TradeEvent::eventId).toList());
    }

    @Test
    void cancellationOfPreviouslyProcessedSellRestoresTheOriginalPosition() {
        service.processEvents(List.of(
                event(31, EventType.BUY, "ACC1", "SEC1", 100),
                event(32, EventType.SELL, "ACC1", "SEC1", 40)
        ));

        service.processEvents(List.of(event(32, EventType.CANCEL, "ACC1", "IGNORED_SECURITY", 0)));

        Position position = service.getPosition("ACC1", "SEC1");
        assertEquals(100, position.quantity());
        assertEquals(List.of(EventType.BUY, EventType.SELL, EventType.CANCEL),
                position.events().stream().map(TradeEvent::eventType).toList());
    }

    @Test
    void rejectsFailedBatchWithoutPublishingSuccessfulGroups() {
        service.processEvents(List.of(event(1, EventType.BUY, "ACC1", "SEC1", 10)));

        assertThrows(TradeProcessingException.class, () -> service.processEvents(List.of(
                event(1, EventType.BUY, "ACC1", "SEC1", 7),
                event(2, EventType.BUY, "ACC2", "SEC2", 7)
        )));

        assertEquals(10, service.getPosition("ACC1", "SEC1").quantity());
        assertThrows(ResourceNotFoundException.class, () -> service.getPosition("ACC2", "SEC2"));
    }

    @Test
    void allowsShortPositionsAsDocumented() {
        service.processEvents(List.of(event(1, EventType.SELL, "ACC1", "SEC1", 8)));

        assertEquals(-8, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void throwsNotFoundWhenNoEventsHaveAffectedThePair() {
        assertThrows(ResourceNotFoundException.class, () -> service.getPosition("ACC1", "SEC1"));
    }

    @Test
    void rejectsNonPositiveBuyQuantity() {
        TradeProcessingException exception = assertThrows(TradeProcessingException.class,
                () -> service.processEvents(List.of(event(1, EventType.BUY, "ACC1", "SEC1", 0))));

        assertEquals(1, exception.getErrors().size());
    }

    @Test
    void rejectsQuantityOverflowWithoutChangingTheCommittedPosition() {
        service.processEvents(List.of(event(1, EventType.BUY, "ACC1", "SEC1", Long.MAX_VALUE)));

        assertThrows(TradeProcessingException.class,
                () -> service.processEvents(List.of(event(2, EventType.BUY, "ACC1", "SEC1", 1))));

        assertEquals(Long.MAX_VALUE, service.getPosition("ACC1", "SEC1").quantity());
    }

    @Test
    void rejectsDuplicateOriginalTradeIdsAcrossDifferentPositionsInOneBatch() {
        assertThrows(IllegalArgumentException.class, () -> service.processEvents(List.of(
                event(1, EventType.BUY, "ACC1", "SEC1", 10),
                event(1, EventType.BUY, "ACC2", "SEC2", 5)
        )));

        assertThrows(ResourceNotFoundException.class, () -> service.getPosition("ACC1", "SEC1"));
        assertThrows(ResourceNotFoundException.class, () -> service.getPosition("ACC2", "SEC2"));
    }

    @Test
    void rejectsPreviouslyUsedTradeIdEvenForDifferentPosition() {
        service.processEvents(List.of(event(1, EventType.BUY, "ACC1", "SEC1", 10)));

        assertThrows(TradeProcessingException.class,
                () -> service.processEvents(List.of(event(1, EventType.BUY, "ACC2", "SEC2", 5))));

        assertThrows(ResourceNotFoundException.class, () -> service.getPosition("ACC2", "SEC2"));
    }

    private static TradeEvent event(long id, EventType type, String account, String security, long quantity) {
        return new TradeEvent(id, type, account, security, quantity);
    }
}
