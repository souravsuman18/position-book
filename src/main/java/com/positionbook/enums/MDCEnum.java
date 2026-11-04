package com.positionbook.enums;

/**
 * Header and logging-context keys used to propagate a request correlation ID.
 */
public enum MDCEnum {

    /** HTTP header used to receive and return a correlation ID. */
    DEFAULT_REQUEST_RESPONSE_TOKEN_HEADER("X-Correlation-Id"),
    /** Logging MDC key under which the active correlation ID is stored. */
    DEFAULT_MDC_UUID_TOKEN_KEY("MDC_CORRELATION_ID");

    private final String value;

    MDCEnum(String value) {
        this.value = value;
    }

    /**
     * Returns the configured header or MDC key.
     *
     * @return the associated string value
     */
    public String getValue() {
        return value;
    }
}
