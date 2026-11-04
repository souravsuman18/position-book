package com.positionbook.exception;

import java.io.Serial;
import java.util.List;

/**
 * Reports validation or business-rule failures encountered while processing a batch.
 */
public class TradeProcessingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Individual messages describing the failed event groups. */
    private final List<String> errors;

    /**
     * Creates a batch failure and retains an immutable copy of its individual errors.
     *
     * @param errors messages describing the failed event groups
     * @param causes exceptions raised by the failed groups
     */
    public TradeProcessingException(List<String> errors, List<? extends Throwable> causes) {
        super("Trade processing failed for one or more account and security combinations.",
                causes.isEmpty() ? null : causes.getFirst());
        this.errors = List.copyOf(errors);
        causes.stream().skip(1).forEach(this::addSuppressed);
    }

    /**
     * Returns the individual batch-processing failure messages.
     *
     * @return immutable list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }
}