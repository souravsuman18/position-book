package com.positionbook.model;

import com.positionbook.enums.EventType;

/**
 * Domain representation of a BUY, SELL, or CANCEL instruction.
 *
 * @param eventId unique original trade ID; a cancellation refers to this ID
 * @param eventType operation to apply
 * @param accountId account affected by the event
 * @param securityId security affected by the event
 * @param quantity trade quantity; cancellation uses the original trade's quantity
 */
public record TradeEvent(
        long eventId,
        EventType eventType,
        String accountId,
        String securityId,
        long quantity) {
}
