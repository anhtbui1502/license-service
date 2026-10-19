package com.vht.license.user;

import com.vht.license.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final AppUserRepository users;

    public CurrentUser(AppUserRepository users) {
        this.users = users;
    }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : null;
    }

    /** Tài khoản đang đăng nhập; ném 401 nếu đã bị xóa/khóa trong lúc còn session. */
    public AppUser get() {
        String username = username();
        AppUser user = username == null ? null : users.findByUsername(username).orElse(null);
        if (user == null || !user.isEnabled()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Phiên đăng nhập không hợp lệ");
        }
        return user;
    }
}
