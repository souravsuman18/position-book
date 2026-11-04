package com.positionbook.exception;

import java.io.Serial;

/**
 * Signals that a requested domain resource does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with details about the missing resource.
     *
     * @param message explanation of what could not be found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}