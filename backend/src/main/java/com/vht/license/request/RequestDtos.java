package com.vht.license.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class RequestDtos {

    private RequestDtos() {
    }

    public record CreateRequest(
            @NotNull Long marketId,
            @NotNull @Positive @Max(10_000_000_000L) Long maxSub,
            @NotNull @Positive @Max(10_000_000_000L) Long maxActiveSub,
            @NotBlank @Size(max = 2000) String reason) {
    }

    public record VerifyRequest(@NotBlank @Pattern(regexp = "\\d{6}", message = "OTP gồm 6 chữ số") String otp) {
    }

    public record PrevLicenseDto(Long id, int version, long maxSub, long maxActiveSub) {
    }

    public record RequestDto(
            Long id,
            String requestCode,
            Long marketId,
            String marketCode,
            String marketName,
            PrevLicenseDto prevLicense,
            long maxSub,
            long maxActiveSub,
            String reason,
            RequestStatus status,
            String requestedBy,
            String requestedByUsername,
            String approverEmail,
            Instant otpExpiresAt,
            Instant otpLastSentAt,
            Instant nextResendAt,
            int resendsLeft,
            int attemptsLeft,
            Instant createdAt,
            Instant closedAt,
            String closedBy,
            Long licenseId,
            /** Người dùng hiện tại có được nhập OTP / gửi lại / hủy không. */
            boolean canAct) {
    }
}
