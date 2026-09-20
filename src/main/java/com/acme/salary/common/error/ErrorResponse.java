package com.acme.salary.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Uniform error envelope: {@code {"error": {"code", "message", "details"}}}. */
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, null));
    }

    public static ErrorResponse of(String code, String message, Map<String, String> details) {
        return new ErrorResponse(new ErrorBody(code, message, details));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String code, String message, Map<String, String> details) {
    }
}
