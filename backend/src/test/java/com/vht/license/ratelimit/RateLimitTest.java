package com.vht.license.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vht.license.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.rules[0].name=verify",
        "app.rate-limit.rules[0].method=POST",
        "app.rate-limit.rules[0].path=/api/requests/*/verify",
        "app.rate-limit.rules[0].key=USER",
        "app.rate-limit.rules[0].capacity=3",
        "app.rate-limit.rules[0].period=1m",
        "app.rate-limit.rules[1].name=default",
        "app.rate-limit.rules[1].path=/api/**",
        "app.rate-limit.rules[1].key=USER",
        "app.rate-limit.rules[1].capacity=1000",
        "app.rate-limit.rules[1].period=1m",
        "app.rate-limit.rules[2].name=login",
        "app.rate-limit.rules[2].method=POST",
        "app.rate-limit.rules[2].path=/api/auth/login",
        "app.rate-limit.rules[2].key=IP",
        "app.rate-limit.rules[2].capacity=2",
        "app.rate-limit.rules[2].period=1m",
})
class RateLimitTest extends IntegrationTestBase {

    @Test
    void verifyEndpointIsRateLimitedPerUser() throws Exception {
        for (int i = 0; i < 3; i++) {
            postJson("/api/requests/999/verify", Map.of("otp", "123456"), "op1").andExpect(status().isNotFound());
        }
        postJson("/api/requests/999/verify", Map.of("otp", "123456"), "op1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
        // user khác có bucket riêng; endpoint khác không bị ảnh hưởng
        postJson("/api/requests/999/verify", Map.of("otp", "123456"), "op2").andExpect(status().isNotFound());
        mvc.perform(as(get("/api/markets"), "op1")).andExpect(status().isOk());
    }

    @Test
    void loginIsRateLimitedPerIp() throws Exception {
        var body = Map.of("username", "nobody", "password", "wrong-password");
        for (int i = 0; i < 2; i++) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                            .contentType("application/json").content(json.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                        .contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isTooManyRequests());
    }
}
