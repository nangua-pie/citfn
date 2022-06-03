package com.forum.common.exception;

import com.forum.common.api.ApiResponse;
import com.forum.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * Single place where every exception is translated into the unified response body.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID = "traceId";

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("business error code={} path={} message={}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        return ApiResponse.failure(ex.getCode(), ex.getMessage()).withTraceId(MDC.get(TRACE_ID));
    }

    /** {@code @Valid} on a request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleBodyValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .collect(Collectors.joining("; "));
        log.warn("request body validation failed: {}", detail);
        return ApiResponse.failure(ResultCode.BAD_REQUEST, detail).withTraceId(MDC.get(TRACE_ID));
    }

    /** {@code @Valid} on form / query object binding. */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindValidation(BindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .collect(Collectors.joining("; "));
        log.warn("request binding validation failed: {}", detail);
        return ApiResponse.failure(ResultCode.BAD_REQUEST, detail).withTraceId(MDC.get(TRACE_ID));
    }

    /** {@code @Validated} on method level parameters. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::describe)
                .collect(Collectors.joining("; "));
        log.warn("constraint violation: {}", detail);
        return ApiResponse.failure(ResultCode.BAD_REQUEST, detail).withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiResponse.failure(ResultCode.BAD_REQUEST, "missing required parameter: " + ex.getParameterName())
                .withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ApiResponse.failure(ResultCode.BAD_REQUEST, "parameter type mismatch: " + ex.getName())
                .withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("unreadable request body: {}", ex.getMessage());
        return ApiResponse.failure(ResultCode.BAD_REQUEST, "malformed request body").withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.failure(ResultCode.BAD_REQUEST, "method not supported: " + ex.getMethod())
                .withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoHandler(NoHandlerFoundException ex) {
        return ApiResponse.failure(ResultCode.NOT_FOUND, "no handler for " + ex.getRequestURL())
                .withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<Void> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
        return ApiResponse.failure(ResultCode.UNAUTHORIZED).withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("access denied on {}", request.getRequestURI());
        return ApiResponse.failure(ResultCode.FORBIDDEN).withTraceId(MDC.get(TRACE_ID));
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Log the full stack trace but never leak internals to the client.
        log.error("unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ApiResponse.failure(ResultCode.INTERNAL_ERROR).withTraceId(MDC.get(TRACE_ID));
    }

    private static String describe(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private static String describe(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }
}
