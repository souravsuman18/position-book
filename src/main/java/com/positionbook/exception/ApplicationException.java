package com.positionbook.exception;

import java.io.Serial;

/**
 * Signals an application-level failure that should be returned as a server error.
 */
public class ApplicationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an application exception with a descriptive message.
     *
     * @param message explanation of the failure
     */
    public ApplicationException(String message) {
        super(message);
    }

    /**
     * Creates an application exception retaining its underlying cause.
     *
     * @param message explanation of the application failure
     * @param cause underlying failure
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

}
