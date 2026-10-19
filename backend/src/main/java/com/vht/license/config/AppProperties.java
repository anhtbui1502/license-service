package com.vht.license.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull ZoneId timezone,
        @Valid @NotNull Approver approver,
        @Valid @NotNull Mail mail,
        @Valid @NotNull Otp otp,
        @Valid @NotNull License license,
        @Valid @NotNull Bootstrap bootstrap) {

    public record Approver(@NotBlank @Email String email, String name) {
    }

    public record Mail(@NotBlank String from) {
    }

    public record Otp(
            @NotNull Duration ttl,
            @NotNull Duration resendCooldown,
            @Min(0) int maxResends,
            @Min(1) int maxAttempts,
            @Min(1) int maxEmailsPerHour,
            @Min(1) int maxEmailsPerHourPerUser,
            @NotBlank @Size(min = 32, message = "app.otp.hmac-secret phải dài tối thiểu 32 ký tự") String hmacSecret) {
    }

    public record License(@NotBlank String crypto) {
    }

    public record Bootstrap(String adminUsername, String adminPassword, String adminEmail) {
    }
}
