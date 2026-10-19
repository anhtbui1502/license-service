package com.vht.license.user;

public enum Role {
    /** Toàn quyền: thị trường, người dùng, audit, và mọi thao tác của OPERATOR. */
    ADMIN,
    /** Tạo yêu cầu, nhập OTP, sinh và tải file license. */
    OPERATOR,
    /** Chỉ xem. */
    VIEWER
}
