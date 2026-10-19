package com.vht.license.crypto;

import java.time.Instant;

/** Dữ liệu đưa vào file license. Định dạng thật do {@link LicenseCryptoProvider} quyết định. */
public record LicensePayload(String marketCode, long maxSub, long maxActiveSub, String licenseCode, int version,
                             Instant issuedAt) {
}
