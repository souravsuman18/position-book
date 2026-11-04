package com.positionbook.api.v1.request;

import com.positionbook.annotation.EnumValue;
import com.positionbook.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One trade event request. CANCEL uses the original trade's eventId; its quantity
 * is ignored and may be zero.
 *
 * @param eventId    original trade identifier, including for CANCEL
 * @param eventType  BUY, SELL, or CANCEL
 * @param accountId  trading account identifier
 * @param securityId security identifier
 * @param quantity   positive for BUY/SELL; ignored for CANCEL
 */
public record TradeEventRequest(
        @NotNull(message = "Event ID is required")
        @Positive(message = "Event ID must be a positive number")
        Long eventId,

        @NotBlank(message = "Event type is required")
        @EnumValue(enumType = EventType.class, message = "Event type must be BUY, SELL, or CANCEL")
        @Schema(allowableValues = {"BUY", "SELL", "CANCEL"})
        String eventType,

        @NotBlank(message = "Account ID is required")
        String accountId,

        @NotBlank(message = "Security ID is required")
        String securityId,

        @NotNull(message = "Quantity is required")
        @PositiveOrZero(message = "Quantity must be a positive number")
        Long quantity
) {
}
