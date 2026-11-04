package com.positionbook.mapper;

import com.positionbook.api.v1.request.TradeEventRequest;
import com.positionbook.enums.EventType;
import com.positionbook.model.TradeEvent;
import org.springframework.stereotype.Component;

/**
 * Converts validated API event requests into domain trade events.
 */
@Component
public class TradeMapper {

    /** Creates the trade-event mapper. */
    public TradeMapper() {
    }

    /**
     * Copies the request fields into the service-layer event model.
     *
     * @param request validated event request
     * @return mapped domain event
     */
    public TradeEvent toModel(TradeEventRequest request) {
        return new TradeEvent(
                request.eventId(),
                EventType.valueOf(request.eventType()),
                request.accountId(),
                request.securityId(),
                request.quantity()
        );
    }
}