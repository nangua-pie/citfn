package com.forum.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.ToString;

/**
 * Unified response envelope for every REST endpoint.
 *
 * <pre>
 * { "code": 0, "message": "success", "data": {}, "traceId": "xxx" }
 * </pre>
 */
@Getter
@ToString
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final String traceId;

    private ApiResponse(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, null);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, data, null);
    }

    public static ApiResponse<Void> failure(ResultCode resultCode) {
        return failure(resultCode.getCode(), resultCode.getMessage());
    }

    /** Failure envelope with no payload, ready to have a trace id attached. */
    public static ApiResponse<Void> failure(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }

    public static ApiResponse<Void> failure(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null, null);
    }

    /** Attaches the current request trace id, used by the exception handler. */
    public ApiResponse<T> withTraceId(String trace) {
        return new ApiResponse<>(code, message, data, trace);
    }
}
