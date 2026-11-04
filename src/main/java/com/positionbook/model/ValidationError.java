package com.positionbook.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A validation detail containing an optional object, field, rejected value, and message.
 */
public class ValidationError {

    private String object;
    private String field;
    @JsonProperty("rejected_value")
    private Object rejectedValue;
    private String message;

    /** Creates an empty validation detail for serialization or binding. */
    public ValidationError() {
    }

    /**
     * Creates an object-level validation detail.
     *
     * @param object name of the object that failed validation
     * @param message validation message
     */
    public ValidationError(final String object, final String message) {
        this.object = object;
        this.message = message;
    }

    /**
     * Creates a validation detail containing only a message.
     *
     * @param message validation message
     */
    public ValidationError(final String message) {
        this.message = message;
    }

    /**
     * Creates a field-level validation detail.
     *
     * @param object name of the object that failed validation
     * @param field field that failed validation
     * @param rejectedValue submitted value that was rejected
     * @param message validation message
     */
    public ValidationError(final String object, final String field, final Object rejectedValue, final String message) {
        this.object = object;
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.message = message;
    }

    /**
     * Returns the name of the object that failed validation, if known.
     *
     * @return object name
     */
    public String getObject() {
        return object;
    }

    /**
     * Sets the name of the object that failed validation.
     *
     * @param object object name
     */
    public void setObject(final String object) {
        this.object = object;
    }

    /**
     * Returns the name of the invalid field, if applicable.
     *
     * @return field name
     */
    public String getField() {
        return field;
    }

    /**
     * Sets the name of the invalid field.
     *
     * @param field field name
     */
    public void setField(final String field) {
        this.field = field;
    }

    /**
     * Returns the submitted value that was rejected, if available.
     *
     * @return rejected value
     */
    public Object getRejectedValue() {
        return rejectedValue;
    }

    /**
     * Sets the submitted value that was rejected.
     *
     * @param rejectedValue rejected value
     */
    public void setRejectedValue(final Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }

    /**
     * Returns the validation message.
     *
     * @return validation message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the validation message.
     *
     * @param message validation message
     */
    public void setMessage(final String message) {
        this.message = message;
    }
}