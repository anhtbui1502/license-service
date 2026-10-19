package com.vht.license.otp;

import com.vht.license.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/** Sinh và kiểm tra OTP. DB chỉ lưu HMAC của OTP, không lưu OTP rõ. */
@Service
public class OtpService {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public OtpService(AppProperties props) {
        this.key = new SecretKeySpec(props.otp().hmacSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /**
     * Băm OTP cùng với nội dung yêu cầu: OTP chỉ hợp lệ cho đúng yêu cầu, đúng thị trường và đúng 2 con số
     * đã gửi trong email.
     */
    public String hash(OtpBinding binding, String otp) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            String input = binding.requestId() + "|" + binding.marketCode() + "|" + binding.maxSub() + "|"
                    + binding.maxActiveSub() + "|" + otp;
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(OtpBinding binding, String otp, String expectedHash) {
        if (otp == null || expectedHash == null) {
            return false;
        }
        byte[] actual = hash(binding, otp).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    public record OtpBinding(long requestId, String marketCode, long maxSub, long maxActiveSub) {
    }
}
