package com.positionbook.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.positionbook.enums.MDCEnum;
import com.positionbook.enums.ResponseStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

/**
 * Standard JSON envelope for successful and failed API responses.
 *
 * <p>The constructor captures the response timestamp and the correlation ID
 * currently associated with the request.</p>
 */
public class ApiResponse {

    private boolean success;
    private String message;
    private Object data;
    private Error error;
    @Schema(
            description = "HTTP status name",
            example = "OK",
            allowableValues = {
                    "OK",
                    "BAD_REQUEST",
                    "UNAUTHORIZED",
                    "FORBIDDEN",
                    "NOT_FOUND",
                    "METHOD_NOT_ALLOWED",
                    "NOT_ACCEPTABLE",
                    "CONFLICT",
                    "UNSUPPORTED_MEDIA_TYPE",
                    "UNPROCESSABLE_ENTITY",
                    "INTERNAL_SERVER_ERROR"
            })
    private HttpStatus status;
    private LocalDateTime timestamp;
    @JsonProperty("correlation_id")
    private String correlationId;
    @Schema(description = "Numeric HTTP status code", example = "200", minimum = "100", maximum = "599")
    private Integer code;

    /**
     * Creates an empty response for JSON serialization or explicit construction.
     */
    public ApiResponse() {
    }

    /**
     * Returns whether the request succeeded.
     *
     * @return {@code true} for a successful response
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the top-level response message.
     *
     * @return response message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the response payload.
     *
     * @return response payload, if present
     */
    public Object getData() {
        return data;
    }

    /**
     * Returns the structured error details, if present.
     *
     * @return error details, or {@code null} for a successful response
     */
    public Error getError() {
        return error == null ? null : error.copy();
    }

    /**
     * Returns the HTTP status associated with the response.
     *
     * @return associated HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns when this response envelope was created.
     *
     * @return response creation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the request correlation ID, if one is available.
     *
     * @return correlation ID, or {@code null} if there is no active request context
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Returns the numeric response code.
     *
     * @return numeric response code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * Sets whether the request succeeded.
     *
     * @param success whether processing succeeded
     */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
     * Sets the top-level response message.
     *
     * @param message response message
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Sets the response payload.
     *
     * @param data response payload
     */
    public void setData(final Object data) {
        this.data = data;
    }

    /**
     * Sets the structured error details.
     *
     * @param error error details
     */
    @SuppressWarnings("PMD.NullAssignment")
    public void setError(final Error error) {
        this.error = error == null ? null : error.copy();
    }

    /**
     * Sets the HTTP status associated with the response.
     *
     * @param status associated HTTP status
     */
    public void setStatus(final HttpStatus status) {
        this.status = status;
    }

    /**
     * Sets the response creation time.
     *
     * @param timestamp response creation timestamp
     */
    public void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the request correlation ID.
     *
     * @param correlationId correlation ID to expose
     */
    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Sets the numeric response code.
     *
     * @param code numeric response code
     */
    public void setCode(final Integer code) {
        this.code = code;
    }

    /**
     * Creates a successful response using the standard success status and message.
     *
     * @param data response payload
     * @return a populated successful response
     */
    public static ApiResponse successResponse(final Object data) {
        ApiResponse response = newResponse();
        response.setData(data);
        response.setStatus(HttpStatus.valueOf(200));
        response.setSuccess(true);
        response.setMessage(ResponseStatusEnum.SUCCESS.getMessage());
        response.setCode(ResponseStatusEnum.SUCCESS.getCode());
        return response;
    }

    /**
     * Creates a successful response with caller-supplied status metadata.
     *
     * @param data response payload
     * @param message response message
     * @param httpStatus HTTP status used for the response
     * @param responseStatusEnum status code metadata
     * @return a populated successful response
     */
    public static ApiResponse successResponse(final Object data, final String message,
                                              final HttpStatus httpStatus, final ResponseStatusEnum responseStatusEnum) {
        ApiResponse response = newResponse();
        response.setData(data);
        response.setStatus(httpStatus);
        response.setSuccess(true);
        response.setMessage(message);
        response.setCode(responseStatusEnum.getCode());
        return response;
    }

    /**
     * Creates a failed response using the supplied error and standard status metadata.
     *
     * @param error error details
     * @param httpStatus HTTP status used for the response
     * @param responseStatusEnum status code and default message
     * @return a populated error response
     */
    public static ApiResponse errorResponse(final Error error, final HttpStatus httpStatus,
                                            final ResponseStatusEnum responseStatusEnum) {
        ApiResponse response = newResponse();
        response.setError(error);
        response.setStatus(httpStatus);
        response.setSuccess(false);
        response.setMessage(responseStatusEnum.getMessage());
        response.setCode(responseStatusEnum.getCode());
        return response;
    }

    /**
     * Creates a failed response using explicit message and numeric code values.
     *
     * @param error error details
     * @param message response message
     * @param httpStatus HTTP status used for the response
     * @param code numeric response code
     * @return a populated error response
     */
    public static ApiResponse errorResponse(final Error error, final String message, final HttpStatus httpStatus,
                                            final Integer code) {
        ApiResponse response = newResponse();
        response.setError(error);
        response.setStatus(httpStatus);
        response.setSuccess(false);
        response.setMessage(message);
        response.setCode(code);
        return response;
    }

    /**
     * Wraps an error model in a response entity using its HTTP status.
     *
     * @param error error details
     * @return response entity containing the standard error envelope
     */
    public static ResponseEntity<Object> generateErrorResponse(final Error error) {
        ApiResponse apiResponse = errorResponse(error, error.getMessage(), error.getStatus(),
                error.getStatus().value());
        return new ResponseEntity<>(apiResponse, error.getStatus());
    }

    /**
     * Wraps a payload in a response entity with HTTP 200.
     *
     * @param data response payload
     * @return response entity containing the standard success envelope
     */
    public static ResponseEntity<Object> generateSuccessResponse(final Object data) {
        ApiResponse apiResponse = successResponse(data);
        return new ResponseEntity<>(apiResponse, HttpStatus.valueOf(200));
    }

    private static ApiResponse newResponse() {
        ApiResponse response = new ApiResponse();
        response.timestamp = LocalDateTime.now();
        response.correlationId = MDC.get(MDCEnum.DEFAULT_MDC_UUID_TOKEN_KEY.getValue());
        return response;
    }

}