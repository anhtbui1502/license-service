package com.vht.license.request;

public enum RequestStatus {
    /** Đã gửi OTP, chờ người tạo nhập OTP. Mỗi thị trường tối đa 1 yêu cầu ở trạng thái này. */
    PENDING_OTP,
    /** OTP đúng, đã sinh file license. */
    GENERATED,
    /** Quá hạn OTP. */
    EXPIRED,
    /** Người tạo hoặc ADMIN hủy. */
    CANCELLED,
    /** Nhập sai OTP quá số lần cho phép. */
    FAILED
}
