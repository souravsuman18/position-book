package com.positionbook.model;

import java.util.List;

/**
 * Current net quantity and accepted event history for an account/security pair.
 *
 * @param accountId account identifier
 * @param securityId security identifier
 * @param quantity current net position; may be negative
 * @param events accepted events affecting this position
 */
public record Position(String accountId, String securityId, long quantity, List<TradeEvent> events) {

    public Position {
        events = List.copyOf(events);
    }

    /**
     * Creates a position with no event history.
     *
     * @param accountId account identifier
     * @param securityId security identifier
     * @param quantity current net position
     */
    public Position(String accountId, String securityId, long quantity) {
        this(accountId, securityId, quantity, List.of());
    }
}
