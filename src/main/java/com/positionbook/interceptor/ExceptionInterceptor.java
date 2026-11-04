package com.positionbook.interceptor;

import com.positionbook.exception.ApplicationException;
import com.positionbook.exception.ResourceNotFoundException;
import com.positionbook.exception.TradeProcessingException;
import com.positionbook.model.ApiResponse;
import com.positionbook.model.ValidationError;
import com.positionbook.property.MessageProperty;
import com.positionbook.util.ExceptionUtil;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.positionbook.model.Error;

/**
 * Converts framework, validation, and business exceptions into the API error envelope.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ExceptionInterceptor extends ResponseEntityExceptionHandler {

    private final String validationError;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Creates the exception handler using the configured validation message.
     *
     * @param messageProperty source of localized application messages
     */
    public ExceptionInterceptor(final MessageProperty messageProperty) {
        validationError = messageProperty.getMessage("default.validation.error");
    }

    /**
     * Handles a constraint violation raised during method validation.
     *
     * @param constraintViolationException the ConstraintViolationException
     * @return a bad-request response containing the validation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(
            final ConstraintViolationException constraintViolationException) {
        final Error error = new Error(HttpStatus.BAD_REQUEST, validationError);
        error.addValidationErrors(constraintViolationException.getConstraintViolations());
        return buildResponseEntity(error, constraintViolationException);
    }

    /**
     * Handle MethodArgumentNotValidException. Triggered when an object fails @Valid
     * validation.
     *
     * @param methodArgumentNotValidException the MethodArgumentNotValidException
     *                                        that is thrown when @Valid
     *                                        validation fails
     * @param headers                         HttpHeaders
     * @param status                          HttpStatus
     * @param request                         WebRequest
     * @return a bad-request response containing the validation details
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException methodArgumentNotValidException,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final Error error = new Error(HttpStatus.BAD_REQUEST, validationError);
        error.addValidationErrors(methodArgumentNotValidException.getBindingResult().getFieldErrors());
        error.addValidationError(methodArgumentNotValidException.getBindingResult().getGlobalErrors());
        return buildResponseEntity(error, methodArgumentNotValidException);
    }

    /**
     * Handles requests whose content type is not supported by the endpoint.
     *
     * @param httpMediaTypeNotSupportedException HttpMediaTypeNotSupportedException
     * @param headers                            HttpHeaders
     * @param status                             HttpStatus
     * @param request                            WebRequest
     * @return an unsupported-media-type response
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            final HttpMediaTypeNotSupportedException httpMediaTypeNotSupportedException,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {

        final String contentType = Optional.ofNullable(httpMediaTypeNotSupportedException.getContentType())
                .map(Object::toString)
                .orElse("Unknown content type");
        final String supportedTypes = httpMediaTypeNotSupportedException.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        final String message = contentType + " media type is not supported. Supported media types are "
                + supportedTypes;
        return buildResponseEntity(new Error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        message, httpMediaTypeNotSupportedException),
                httpMediaTypeNotSupportedException);
    }

    /**
     * Handle HttpMediaTypeNotAcceptableException.
     *
     * @param httpMediaTypeNotAcceptableException HttpMediaTypeNotAcceptableException
     * @param headers                             HttpHeaders
     * @param status                              HttpStatus
     * @param request                             WebRequest
     * @return an unsupported-media-type response
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            final HttpMediaTypeNotAcceptableException httpMediaTypeNotAcceptableException,
            final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {

        final StringBuilder builder = new StringBuilder(
                "MIME type is not supported. Supported MIME type's are ");
        httpMediaTypeNotAcceptableException.getSupportedMediaTypes()
                .forEach(t -> builder.append(t).append(", "));
        return buildResponseEntity(new Error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        builder.substring(0, builder.length() - 2), httpMediaTypeNotAcceptableException),
                httpMediaTypeNotAcceptableException);
    }

    /**
     * Handle NoHandlerFoundException.
     *
     * @param noHandlerFoundException NoHandlerFoundException
     * @param headers                 HttpHeaders
     * @param status                  HttpStatus
     * @param request                 WebRequest
     * @return a not-found response describing the unmatched route
     */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            final NoHandlerFoundException noHandlerFoundException, final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {
        final Error error = new Error(HttpStatus.NOT_FOUND, "No handler found for "
                + noHandlerFoundException.getHttpMethod() + " "
                + noHandlerFoundException.getRequestURL(),
                noHandlerFoundException);
        return buildResponseEntity(error, noHandlerFoundException);
    }

    /**
     * Handle HttpRequestMethodNotSupportedException.
     *
     * @param httpRequestMethodNotSupportedException HttpRequestMethodNotSupportedException
     * @param headers                                HttpHeaders
     * @param status                                 HttpStatus
     * @param request                                WebRequest
     * @return a method-not-allowed response
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            final HttpRequestMethodNotSupportedException httpRequestMethodNotSupportedException,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {

        final String message = httpRequestMethodNotSupportedException.getMethod()
                + " method is not supported for this request. Supported method's are "
                + httpRequestMethodNotSupportedException.getSupportedHttpMethods();

        final Error error = new Error(HttpStatus.METHOD_NOT_ALLOWED, message,
                httpRequestMethodNotSupportedException);
        return buildResponseEntity(error, httpRequestMethodNotSupportedException);
    }

    /**
     * Handle HttpMessageNotReadableException. Happens when request JSON is
     * malformed.
     *
     * @param httpMessageNotReadableException HttpMessageNotReadableException
     * @param headers                         HttpHeaders
     * @param status                          HttpStatus
     * @param request                         WebRequest
     * @return a bad-request response describing the malformed body
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException httpMessageNotReadableException,
            final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        return buildResponseEntity(
                new Error(HttpStatus.BAD_REQUEST, "Malformed JSON request",
                        httpMessageNotReadableException),
                httpMessageNotReadableException);
    }

    /**
     * Handle HttpMessageNotWritableException.
     *
     * @param httpMessageNotWritableException HttpMessageNotWritableException
     * @param headers                         HttpHeaders
     * @param status                          HttpStatus
     * @param request                         WebRequest
     * @return an internal-server-error response
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            final HttpMessageNotWritableException httpMessageNotWritableException,
            final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        return buildResponseEntity(new Error(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing JSON output",
                httpMessageNotWritableException), httpMessageNotWritableException);
    }

    /**
     * Handle MethodArgumentTypeMismatchException.
     *
     * @param methodArgumentTypeMismatchException MethodArgumentTypeMismatchException
     * @return a bad-request response describing the type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
        final String errorMessage = String.format("%1$s should be of type %2$s",
                methodArgumentTypeMismatchException.getName(),
                Optional.ofNullable(methodArgumentTypeMismatchException.getRequiredType())
                        .map(Class::getName)
                        .orElse(""));
        final Error error = new Error(HttpStatus.BAD_REQUEST, errorMessage,
                methodArgumentTypeMismatchException);
        return buildResponseEntity(error, methodArgumentTypeMismatchException);
    }

    /**
     * Handle TypeMismatchException.
     *
     * @param typeMismatchException TypeMismatchException
     * @param headers               HttpHeaders
     * @param status                HttpStatus
     * @param request               WebRequest
     * @return a bad-request response describing the type mismatch
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(final TypeMismatchException typeMismatchException,
                                                        final HttpHeaders headers, final HttpStatusCode status,
                                                        final WebRequest request) {
        final String errorMessage = String.format("%1$s should be of type %2$s",
                typeMismatchException.getPropertyName(),
                Optional.ofNullable(typeMismatchException.getRequiredType())
                        .map(Class::getName)
                        .orElse(""));
        final Error error = new Error(HttpStatus.BAD_REQUEST, errorMessage, typeMismatchException);
        return buildResponseEntity(error, typeMismatchException);
    }

    /**
     * Handle MissingServletRequestParameterException.
     *
     * @param missingServletRequestParameterException MissingServletRequestParameterException
     * @param headers                                 HttpHeaders
     * @param status                                  HttpStatus
     * @param request                                 WebRequest
     * @return a bad-request response describing the missing parameter
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            final MissingServletRequestParameterException missingServletRequestParameterException,
            final HttpHeaders headers, final HttpStatusCode status, final WebRequest request) {
        final String errorMessage = missingServletRequestParameterException.getParameterName()
                + " parameter is missing";
        final Error error = new Error(HttpStatus.BAD_REQUEST, errorMessage,
                missingServletRequestParameterException);
        return buildResponseEntity(error, missingServletRequestParameterException);
    }

    /**
     * Converts request-binding failures into a bad-request error response.
     *
     * @param servletRequestBindingException request-binding failure
     * @param headers response headers
     * @param status framework-provided status
     * @param request current web request
     * @return a bad-request response
     */
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            final ServletRequestBindingException servletRequestBindingException, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final Error error = new Error(HttpStatus.BAD_REQUEST, "",
                servletRequestBindingException);
        return buildResponseEntity(error, servletRequestBindingException);
    }

    /**
     * Handle MissingServletRequestPartException.
     *
     * @param missingServletRequestPartException MissingServletRequestPartException
     * @param headers                            HttpHeaders
     * @param status                             HttpStatus
     * @param request                            WebRequest
     * @return a bad-request response describing the missing request part
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            final MissingServletRequestPartException missingServletRequestPartException,
            final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final String errorMessage = missingServletRequestPartException.getRequestPartName()
                + " request part is missing";
        final Error error = new Error(HttpStatus.BAD_REQUEST, errorMessage,
                missingServletRequestPartException);
        return buildResponseEntity(error, missingServletRequestPartException);
    }

    /**
     * Handle MissingPathVariableException.
     *
     * @param missingPathVariableException MissingPathVariableException
     * @param headers                      HttpHeaders
     * @param status                       HttpStatus
     * @param request                      WebRequest
     * @return a bad-request response describing the missing path variable
     */
    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(
            final MissingPathVariableException missingPathVariableException, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final String errorMessage = missingPathVariableException.getVariableName() + " of "
                + missingPathVariableException.getParameter().getParameterName() + " path is missing";
        final Error error = new Error(HttpStatus.BAD_REQUEST, errorMessage,
                missingPathVariableException);
        return buildResponseEntity(error, missingPathVariableException);
    }

    /**
     * Converts application-level failures into internal-server-error responses.
     *
     * @param applicationException application failure to report
     * @return an internal-server-error response
     */
    @ExceptionHandler(ApplicationException.class)
    protected ResponseEntity<Object> handleApplicationException(final ApplicationException applicationException) {
        final Error error = new Error(HttpStatus.INTERNAL_SERVER_ERROR,
                applicationException.getLocalizedMessage() + (null != applicationException.getCause()
                        ? applicationException.getCause().getLocalizedMessage()
                        : ""));
        return buildResponseEntity(error, applicationException);
    }

    /**
     * Converts missing resources into a not-found response.
     *
     * @param exception missing-resource failure
     * @return a not-found response describing the missing resource
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFoundException(
            final ResourceNotFoundException exception) {
        final Error error = new Error(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        return buildResponseEntity(error, exception);
    }

    /**
     * Logs client disconnects, for which a response can no longer be delivered.
     *
     * @param clientAbortException exception raised after the client disconnects
     * @param request current web request
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortExceptionHandler(final ClientAbortException clientAbortException,
                                                  final WebRequest request) {

        if (log.isWarnEnabled()) {
            log.warn("Connection was forcibly closed. Requested Url: {}, Closed Time: {}, Exception Detail: {}",
                    ((ServletWebRequest) request).getRequest().getRequestURI(),
                    DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()),
                    clientAbortException.getMessage());
        }
    }

    /**
     * Converts exceptions handled by the framework into the common error envelope.
     *
     * @param exception exception being handled
     * @param body framework-provided response body, if any
     * @param headers response headers
     * @param status HTTP status selected by the framework
     * @param request current web request
     * @return response entity containing the common error envelope
     */
    @Override
    public ResponseEntity<Object> handleExceptionInternal(final Exception exception, final Object body,
                                                          final HttpHeaders headers, final HttpStatusCode status,
                                                          final WebRequest request) {
        final Error error = new Error(HttpStatus.valueOf(status.value()), exception);
        return buildResponseEntity(error, exception);
    }

    /**
     * Handle Exception.
     *
     * @param exception Exception
     * @return an internal-server-error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(final Exception exception) {
        final Error error = new Error(HttpStatus.INTERNAL_SERVER_ERROR, exception);
        return buildResponseEntity(error, exception);
    }

    /**
     * Converts batch-processing failures into an unprocessable-entity response.
     *
     * @param ex batch failure and its individual group errors
     * @return an unprocessable-entity response containing each processing error
     */
    @ExceptionHandler(TradeProcessingException.class)
    public ResponseEntity<Object> handleTradeEventProcessing(TradeProcessingException ex) {

        final Error error = new Error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getLocalizedMessage(), ex);
        // Preserve each failed group as a separate detail in the response.
        List<ValidationError> validationErrors = ex.getErrors().stream()
                .map(errorMsg -> new ValidationError(errorMsg))
                .collect(Collectors.toList());
        error.setSubErrors(validationErrors);

        return buildResponseEntity(error, ex);
    }

    /**
     * Converts validation failures on handler method parameters into a bad request.
     *
     * @param exception method-parameter validation failure
     * @param headers response headers
     * @param status framework-provided status
     * @param request current web request
     * @return a bad-request response containing the validation details
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            final HandlerMethodValidationException exception,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {

        final Error error = new Error(HttpStatus.BAD_REQUEST, validationError);

        final List<ValidationError> validationErrors =
                exception.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream())
                        .map(validationError ->
                                new ValidationError(validationError.getDefaultMessage()))
                        .toList();

        error.setSubErrors(validationErrors);

        return buildResponseEntity(error, exception);
    }

    /**
     * Handles IllegalArgumentException and returns a response entity with a bad request status.
     * @param ex the IllegalArgumentException to handle
     * @return a ResponseEntity containing the error details and a bad request status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        final Error error = new Error(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), ex);
        return buildResponseEntity(error, ex);
    }

    /**
     * Handles IllegalStateException and returns a response entity with a conflict status.
     * @param ex the IllegalStateException to handle
     * @return a ResponseEntity containing the error details and a conflict status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
        final Error error = new Error(HttpStatus.CONFLICT, ex.getLocalizedMessage(), ex);
        return buildResponseEntity(error, ex);
    }

    /**
     * Logs an exception at severity appropriate to its status and builds its response.
     *
     * @param error structured API error to return
     * @param exception exception to log
     * @return response entity containing the standard error envelope
     */
    private ResponseEntity<Object> buildResponseEntity(final Error error, final Exception exception) {
        if (error.getStatus().is5xxServerError()) {
            if (log.isErrorEnabled())
                log.error("Exception: {} at {}", exception.getMessage(),
                        ExceptionUtil.getCleanStackTrace(exception));
        } else {
            if (log.isWarnEnabled())
                log.warn("Warning: {} ", exception.getMessage());
        }

        return ApiResponse.generateErrorResponse(error);
    }
}