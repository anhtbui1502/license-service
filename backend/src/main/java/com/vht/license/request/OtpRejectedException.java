package com.vht.license.request;

import com.vht.license.common.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * OTP bị từ chối (sai / hết hạn / bị khóa). Transaction KHÔNG rollback khi ném lỗi này
 * để lưu lại số lần nhập sai và trạng thái mới của yêu cầu.
 */
public class OtpRejectedException extends BusinessException {

    public OtpRejectedException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
