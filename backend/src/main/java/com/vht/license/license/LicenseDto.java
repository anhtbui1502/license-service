package com.vht.license.license;

import java.time.Instant;

public record LicenseDto(Long id, String licenseCode, Long marketId, String marketCode, String marketName,
                         int version, long maxSub, long maxActiveSub, String fileName, String fileSha256,
                         String cryptoName, boolean current, Long requestId, String requestCode, String issuedBy,
                         Instant issuedAt) {

    public static LicenseDto of(LicenseFile l) {
        return new LicenseDto(l.getId(), l.getLicenseCode(), l.getMarket().getId(), l.getMarket().getCode(),
                l.getMarket().getName(), l.getVersion(), l.getMaxSub(), l.getMaxActiveSub(), l.getFileName(),
                l.getFileSha256(), l.getCryptoName(), l.isCurrent(),
                l.getRequest() == null ? null : l.getRequest().getId(),
                l.getRequest() == null ? null : l.getRequest().getRequestCode(),
                l.getIssuedBy() == null ? null : l.getIssuedBy().getDisplayName(), l.getIssuedAt());
    }
}
