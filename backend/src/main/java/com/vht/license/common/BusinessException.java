package com.vht.license.common;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ trả về client dưới dạng {code, message}. */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException notFound(String what) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy " + what);
    }

    public static BusinessException badRequest(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
