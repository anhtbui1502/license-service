package com.vht.license.otp;

public interface OtpNotifier {

    /** Gửi OTP tới người duyệt; ném exception nếu gửi thất bại. */
    void send(OtpEmail email);
}
