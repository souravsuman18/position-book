package com.positionbook.service;

import com.positionbook.model.Position;
import com.positionbook.model.TradeEvent;

import java.util.List;

/**
 * Operations for updating and querying the in-memory position book.
 */
public interface IPositionBookService {

    /**
     * Processes a batch of events atomically and returns each affected position.
     *
     * @param tradeEvents events to apply
     * @return resulting positions in first-seen account/security order
     * @throws IllegalArgumentException if the batch contains duplicate original trade IDs
     *         or duplicate cancellations for an event ID
     * @throws com.positionbook.exception.TradeProcessingException if any event group fails
     */
    List<Position> processEvents(List<TradeEvent> tradeEvents);

    /**
     * Finds the current position for an account and security.
     *
     * @param accountId account identifier
     * @param securityId security identifier
     * @return current position for the pair
     * @throws com.positionbook.exception.ResourceNotFoundException if no position has been recorded
     */
    Position getPosition(String accountId, String securityId);
}
