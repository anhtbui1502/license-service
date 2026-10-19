package com.vht.license.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled, List<Rule> rules) {

    public RateLimitProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public enum KeyType {
        /** Theo địa chỉ IP client. */
        IP,
        /** Theo tài khoản đăng nhập; chưa đăng nhập thì theo IP. */
        USER
    }

    /**
     * @param method HTTP method, để trống = mọi method
     * @param path   Ant pattern, ví dụ /api/requests/{@literal *}/verify
     */
    public record Rule(String name, String method, String path, KeyType key, long capacity, Duration period) {
    }
}
