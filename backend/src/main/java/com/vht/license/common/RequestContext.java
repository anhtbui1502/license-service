package com.vht.license.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestContext {

    private RequestContext() {
    }

    /** IP client của request hiện tại (đã xử lý X-Forwarded-For bởi Tomcat RemoteIpValve). */
    public static String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
