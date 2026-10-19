package com.vht.license.market;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class MarketDtos {

    private MarketDtos() {
    }

    public record CurrentLicenseDto(Long id, String licenseCode, int version, long maxSub, long maxActiveSub,
                                    Instant issuedAt) {
    }

    public record PendingRequestDto(Long id, String requestCode, long maxSub, long maxActiveSub, String requestedBy,
                                    Instant otpExpiresAt) {
    }

    public record MarketDto(Long id, String code, String name, String country, boolean licenseRequired,
                            boolean active, String note, Instant createdAt, CurrentLicenseDto currentLicense,
                            PendingRequestDto pendingRequest) {
    }

    public record CreateMarketRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9_-]+", message = "chỉ gồm chữ in hoa, số, _ -")
            String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 100) String country,
            boolean licenseRequired,
            @Size(max = 1000) String note) {
    }

    public record UpdateMarketRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 100) String country,
            boolean licenseRequired,
            boolean active,
            @Size(max = 1000) String note) {
    }
}
