package com.positionbook.model;

import java.util.Map;

/**
 * Staged result for processing all events associated with one position key.
 *
 * @param position resulting position after the group's events
 * @param originalTrades new BUY/SELL events to add to trade history
 * @param cancelledTrades new cancellations to record
 */
public record GroupResult(
        Position position,
        Map<Long, TradeEvent> originalTrades,
        Map<Long, TradeEvent> cancelledTrades) {

    public GroupResult {
        originalTrades = Map.copyOf(originalTrades);
        cancelledTrades = Map.copyOf(cancelledTrades);
    }
}
