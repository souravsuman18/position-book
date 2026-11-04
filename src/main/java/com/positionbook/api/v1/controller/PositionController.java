package com.positionbook.api.v1.controller;

import com.positionbook.api.v1.request.TradeEventRequest;
import com.positionbook.mapper.TradeMapper;
import com.positionbook.model.ApiResponse;
import com.positionbook.model.TradeEvent;
import com.positionbook.service.IPositionBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP endpoints for submitting trade events and retrieving current positions.
 */
@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Positions and events", description = "Submit trade events and retrieve current positions.")
public class PositionController {

    private final IPositionBookService service;
    private final TradeMapper tradeMapper;

    /**
     * Creates the controller with its business service and request mapper.
     *
     * @param service service that processes events and retrieves positions
     * @param tradeMapper converts API requests into domain events
     */
    public PositionController(IPositionBookService service,
                              TradeMapper tradeMapper) {
        this.service = service;
        this.tradeMapper = tradeMapper;
    }

    /**
     * Processes a batch of trade events.
     *
     * @param events validated event requests
     * @return the standard API response containing the affected positions
     */
    @PostMapping("/events")
    @Operation(summary = "Process trade events",
            description = "Processes a batch atomically and returns updated positions with accepted event history.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Events processed successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request fields or duplicate original event IDs"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "One or more trade events could not be processed",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ApiResponse.class)))
    })
    public ApiResponse processEvent(@Valid @RequestBody List<TradeEventRequest> events) {
        List<TradeEvent> tradeEvents = events.stream().map(tradeMapper::toModel).toList();
        return ApiResponse.successResponse(service.processEvents(tradeEvents));
    }

    /**
     * Retrieves the current position for an account and security pair.
     *
     * @param accountId account identifier
     * @param securityId security identifier
     * @return the standard API response containing the requested position
     */
    @GetMapping("/positions/{accountId}/{securityId}")
    @Operation(summary = "Get a position",
            description = "Returns the current position and event history. Returns 404 if the position does not exist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Position retrieved successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "No position exists for the account and security combination",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ApiResponse.class)))
    })
    public ApiResponse getPosition(@PathVariable String accountId, @PathVariable String securityId) {
        return ApiResponse.successResponse(service.getPosition(accountId, securityId));
    }
}
