package com.vht.license.crypto;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Định dạng TẠM: properties mã hóa Base64, KHÔNG an toàn. Chỉ dùng khi chưa có thuật toán thật.
 */
public class MockLicenseCryptoProvider implements LicenseCryptoProvider {

    private static final String HEADER = "MOCK-LICENSE-V1\n";

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String fileExtension() {
        return "lic";
    }

    @Override
    public byte[] encrypt(LicensePayload p) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("marketCode", p.marketCode());
        values.put("maxSub", Long.toString(p.maxSub()));
        values.put("maxActiveSub", Long.toString(p.maxActiveSub()));
        values.put("licenseCode", p.licenseCode());
        values.put("version", Integer.toString(p.version()));
        values.put("issuedAt", p.issuedAt().toString());
        StringBuilder sb = new StringBuilder();
        values.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
        String body = Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
        return (HEADER + body).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public LicensePayload decrypt(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (!text.startsWith(HEADER)) {
            throw new IllegalArgumentException("Không phải file license mock");
        }
        Properties props = new Properties();
        try {
            props.load(new java.io.StringReader(new String(
                    Base64.getDecoder().decode(text.substring(HEADER.length()).trim()), StandardCharsets.UTF_8)));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException(e);
        }
        return new LicensePayload(props.getProperty("marketCode"), Long.parseLong(props.getProperty("maxSub")),
                Long.parseLong(props.getProperty("maxActiveSub")), props.getProperty("licenseCode"),
                Integer.parseInt(props.getProperty("version")), Instant.parse(props.getProperty("issuedAt")));
    }
}
