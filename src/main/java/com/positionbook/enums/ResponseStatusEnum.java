package com.positionbook.enums;

/**
 * HTTP response codes and default messages exposed by the API response envelope.
 */
public enum ResponseStatusEnum {

    /** Request completed successfully. */
    SUCCESS(200, "Success"),
    /** Request data is invalid. */
    BAD_REQUEST(400, "Bad Request"),
    /** Authentication is required or failed. */
    UNAUTHORIZED(401, "Unauthorized"),
    /** The caller is not permitted to perform the operation. */
    FORBIDDEN(403, "Forbidden"),
    /** The requested resource was not found. */
    NOT_FOUND(404, "Not Found"),
    /** An unexpected server-side failure occurred. */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String message;

    /**
     * Associates a response status with its numeric code and display message.
     *
     * @param code numeric status code
     * @param message default status message
     */
    ResponseStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Returns the numeric response code.
     *
     * @return HTTP-style status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the default response message.
     *
     * @return human-readable status message
     */
    public String getMessage() {
        return message;
    }
}
