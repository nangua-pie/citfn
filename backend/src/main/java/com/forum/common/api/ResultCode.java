package com.forum.common.api;

/**
 * Application level result codes returned in {@link ApiResponse#getCode()}.
 */
public enum ResultCode {

    /** Request succeeded. */
    SUCCESS(0, "success"),

    /** Invalid or missing request parameter. */
    BAD_REQUEST(40000, "invalid request parameter"),

    /** Credentials missing, malformed or expired. */
    UNAUTHORIZED(40100, "not logged in"),

    /** Authenticated but lacking the required permission. */
    FORBIDDEN(40300, "permission denied"),

    /** Target resource does not exist. */
    NOT_FOUND(40400, "resource not found"),

    /** Business rule rejected the operation. */
    CONFLICT(40900, "business conflict"),

    /** Rate limit or quota exceeded. */
    TOO_MANY_REQUESTS(42900, "too many requests"),

    /** Unexpected server side failure. */
    INTERNAL_ERROR(50000, "internal server error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
