package com.positionbook.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.ConstraintViolation;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Error details used by the API response envelope.
 *
 * <p>The HTTP status and internal message are excluded from JSON; localized
 * text and structured validation errors are exposed instead.</p>
 */
public class Error {

    @JsonIgnore
    private HttpStatus status;
    @JsonProperty("localized_message")
    private String localizedMessage;
    @JsonIgnore
    private String message;
    @JsonProperty("sub_errors")
    private List<ValidationError> subErrors = new ArrayList<>();

    /** Creates an error model with an empty list of validation details. */
    public Error() {
    }

    Error copy() {
        Error copy = new Error(status, localizedMessage);
        copy.message = message;
        copy.subErrors = subErrors.stream()
                .map(validationError -> new ValidationError(
                        validationError.getObject(),
                        validationError.getField(),
                        validationError.getRejectedValue(),
                        validationError.getMessage()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return copy;
    }

    private void addValidationError(final ConstraintViolation<?> constraintViolation) {
        this.addValidationError(constraintViolation.getRootBeanClass().getSimpleName(),
                ((PathImpl) constraintViolation.getPropertyPath()).getLeafNode().asString(),
                constraintViolation.getInvalidValue(), constraintViolation.getMessage());
    }

    private void addValidationError(final FieldError fieldError) {
        this.addValidationError(fieldError.getObjectName(), fieldError.getField(), fieldError.getRejectedValue(),
                fieldError.getDefaultMessage());
    }

    private void addValidationError(final ObjectError objectError) {
        this.addValidationError(objectError.getObjectName(), objectError.getDefaultMessage());
    }

    private void addValidationError(final String object, final String field, final Object rejectedValue,
                                    final String errorMessage) {
        addValidationError(new ValidationError(object, field, rejectedValue, errorMessage));
    }

    private void addValidationError(final String object, final String errorMessage) {
        addValidationError(new ValidationError(object, errorMessage));
    }

    /**
     * Creates an error with an HTTP status.
     *
     * @param status HTTP status associated with the error
     */
    public Error(final HttpStatus status) {
        this();
        this.status = status;
    }

    /**
     * Creates an error with an HTTP status and localized message.
     *
     * @param status HTTP status associated with the error
     * @param localizedMessage client-facing localized message
     */
    public Error(final HttpStatus status, final String localizedMessage) {
        this();
        this.status = status;
        this.localizedMessage = localizedMessage;
    }

    /**
     * Creates an error from a throwable, retaining its message for internal use.
     *
     * @param status HTTP status associated with the error
     * @param throwable cause of the error
     */
    public Error(final HttpStatus status, final Throwable throwable) {
        this();
        this.status = status;
        this.localizedMessage = "Unexpected Error";
        this.message = throwable.getLocalizedMessage();
    }

    /**
     * Creates an error with localized text and a throwable cause.
     *
     * @param status HTTP status associated with the error
     * @param localizedMessage client-facing localized message
     * @param throwable cause of the error
     */
    public Error(final HttpStatus status, final String localizedMessage, final Throwable throwable) {
        this();
        this.status = status;
        this.localizedMessage = localizedMessage;
        this.message = throwable.getLocalizedMessage();
    }

    /**
     * Returns the client-facing localized error message.
     *
     * @return localized message
     */
    public String getLocalizedMessage() {
        return localizedMessage;
    }

    /**
     * Sets the client-facing localized error message.
     *
     * @param localizedMessage localized message
     */
    public void setLocalizedMessage(final String localizedMessage) {
        this.localizedMessage = localizedMessage;
    }

    /**
     * Returns the internal error message.
     *
     * @return internal error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the internal error message.
     *
     * @param message internal error message
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Returns the HTTP status associated with this error.
     *
     * @return associated HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Sets the HTTP status associated with this error.
     *
     * @param status associated HTTP status
     */
    public void setStatus(final HttpStatus status) {
        this.status = status;
    }

    /**
     * Returns a defensive copy of the structured validation details.
     *
     * @return validation details
     */
    public List<ValidationError> getSubErrors() {
        return new ArrayList<>(subErrors);
    }

    /**
     * Replaces the validation details with an immutable copy of the supplied list.
     *
     * @param subErrors validation details to store
     */
    public void setSubErrors(final List<ValidationError> subErrors) {
        if (null != subErrors)
            this.subErrors = List.copyOf(subErrors);
    }

    /**
     * Adds each bean-validation violation as a structured validation detail.
     *
     * @param constraintViolations validation violations to append
     */
    public void addValidationErrors(final Set<ConstraintViolation<?>> constraintViolations) {
        constraintViolations.forEach(this::addValidationError);
    }

    /**
     * Adds each field-binding failure as a structured validation detail.
     *
     * @param fieldErrors field errors to append
     */
    public void addValidationErrors(final List<FieldError> fieldErrors) {
        fieldErrors.forEach(this::addValidationError);
    }

    /**
     * Appends a single structured validation detail.
     *
     * @param validationError detail to append
     */
    public void addValidationError(final ValidationError validationError) {
        subErrors.add(validationError);
    }

    /**
     * Adds each global binding failure as a structured validation detail.
     *
     * @param globalErrors global object errors to append
     */
    public void addValidationError(final List<ObjectError> globalErrors) {
        globalErrors.forEach(this::addValidationError);
    }
}