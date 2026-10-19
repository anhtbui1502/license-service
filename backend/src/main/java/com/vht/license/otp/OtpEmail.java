package com.vht.license.otp;

import java.time.Instant;

/** Nội dung email OTP gửi người duyệt. currentXxx = null khi thị trường chưa có license. */
public record OtpEmail(
        String to,
        String requestCode,
        String marketCode,
        String marketName,
        String requestedBy,
        String reason,
        Integer currentVersion,
        Long currentMaxSub,
        Long currentMaxActiveSub,
        long newMaxSub,
        long newMaxActiveSub,
        String otp,
        Instant expiresAt,
        boolean resend) {

    @Override
    public String toString() {
        // Không để OTP lọt vào log
        return "OtpEmail[to=" + to + ", requestCode=" + requestCode + ", market=" + marketCode + ", resend=" + resend + "]";
    }
}
