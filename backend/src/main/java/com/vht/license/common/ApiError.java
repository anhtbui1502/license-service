package com.vht.license.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Long retryAfterSeconds) {

    public ApiError(String code, String message) {
        this(code, message, null);
    }
}
