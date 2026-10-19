package com.vht.license.common;

import org.springframework.http.HttpStatus;

/** Lỗi 429 kèm thời gian phải chờ trước khi thử lại. */
public class RetryableBusinessException extends BusinessException {

    private final long retryAfterSeconds;

    public RetryableBusinessException(String code, String message, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
