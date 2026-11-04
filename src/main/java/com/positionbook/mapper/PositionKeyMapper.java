package com.positionbook.mapper;

import com.positionbook.model.PositionKey;
import com.positionbook.model.TradeEvent;
import org.springframework.stereotype.Component;

/**
 * Builds position keys from trade events or account/security identifiers.
 */
@Component
public class PositionKeyMapper {

    /** Creates the position-key mapper. */
    public PositionKeyMapper() {
    }

    /**
     * Extracts the account and security identifiers from an event.
     *
     * @param tradeEvent event whose position key is needed
     * @return the corresponding position key
     */
    public PositionKey toModel(TradeEvent tradeEvent) {
        return new PositionKey(
                tradeEvent.accountId(),
                tradeEvent.securityId()
        );
    }

    /**
     * Creates a position key from its component identifiers.
     *
     * @param accountId account identifier
     * @param securityId security identifier
     * @return the corresponding position key
     */
    public PositionKey toModel(String accountId, String securityId) {
        return new PositionKey(
                accountId,
                securityId
        );
    }
}
