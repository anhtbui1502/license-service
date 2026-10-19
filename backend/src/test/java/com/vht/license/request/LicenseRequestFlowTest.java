package com.vht.license.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.vht.license.IntegrationTestBase;
import com.vht.license.crypto.LicensePayload;
import com.vht.license.crypto.MockLicenseCryptoProvider;
import com.vht.license.otp.OtpEmail;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LicenseRequestFlowTest extends IntegrationTestBase {

    @Autowired
    LicenseRequestService service;

    private String wrongOtp(String otp) {
        return otp.equals("000000") ? "111111" : "000000";
    }

    @Test
    void happyPath_createVerifyDownload() throws Exception {
        JsonNode req = body(createRequest(mm.getId(), 500_000, 300_000, "op1").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_OTP"))
                .andExpect(jsonPath("$.canAct").value(true)));
        long id = req.get("id").asLong();

        OtpEmail email = lastEmail();
        assertThat(email.to()).isEqualTo("director@test.local");
        assertThat(email.marketCode()).isEqualTo("MM");
        assertThat(email.newMaxSub()).isEqualTo(500_000);
        assertThat(email.newMaxActiveSub()).isEqualTo(300_000);
        assertThat(email.currentVersion()).isNull();
        assertThat(email.otp()).matches("\\d{6}");

        // OTP không lưu dạng rõ
        String stored = jdbc.queryForObject("select otp_hash from license_request where id = ?", String.class, id);
        assertThat(stored).isNotEqualTo(email.otp()).hasSize(64);

        JsonNode lic = body(postJson("/api/requests/" + id + "/verify", Map.of("otp", email.otp()), "op1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.current").value(true)));

        byte[] file = mvc.perform(as(get("/api/licenses/" + lic.get("id").asLong() + "/download"), "op1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Checksum-SHA256"))
                .andReturn().getResponse().getContentAsByteArray();
        LicensePayload payload = new MockLicenseCryptoProvider().decrypt(file);
        assertThat(payload.marketCode()).isEqualTo("MM");
        assertThat(payload.maxSub()).isEqualTo(500_000);
        assertThat(payload.maxActiveSub()).isEqualTo(300_000);

        // Lần 2: v2, v1 không còn là current, email có số hiện hành
        long id2 = body(createRequest(mm.getId(), 700_000, 450_000, "op1").andExpect(status().isOk()))
                .get("id").asLong();
        OtpEmail email2 = lastEmail();
        assertThat(email2.currentVersion()).isEqualTo(1);
        assertThat(email2.currentMaxSub()).isEqualTo(500_000);
        postJson("/api/requests/" + id2 + "/verify", Map.of("otp", email2.otp()), "op1")
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        assertThat(jdbc.queryForObject("select count(*) from license_file where market_id = ? and is_current",
                Integer.class, mm.getId())).isEqualTo(1);
    }

    @Test
    void onlyOnePendingRequestPerMarket() throws Exception {
        createRequest(mm.getId(), 500_000, 300_000, "op1").andExpect(status().isOk());
        createRequest(mm.getId(), 600_000, 300_000, "op2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDING_EXISTS"));
        // thị trường khác không bị ảnh hưởng
        createRequest(th.getId(), 100_000, 50_000, "op2").andExpect(status().isOk());
        verify(notifier, times(2)).send(any());
    }

    @Test
    void cancelFreesSlot() throws Exception {
        long id = body(createRequest(mm.getId(), 500_000, 300_000, "op1")).get("id").asLong();
        postJson("/api/requests/" + id + "/cancel", Map.of(), "op2").andExpect(status().isForbidden());
        postJson("/api/requests/" + id + "/cancel", Map.of(), "op1")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        createRequest(mm.getId(), 600_000, 300_000, "op2").andExpect(status().isOk());
    }

    @Test
    void wrongOtpLocksRequestAfterMaxAttempts() throws Exception {
        long id = body(createRequest(mm.getId(), 500_000, 300_000, "op1")).get("id").asLong();
        String bad = wrongOtp(lastEmail().otp());
        for (int i = 1; i <= 4; i++) {
            postJson("/api/requests/" + id + "/verify", Map.of("otp", bad), "op1")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("OTP_INVALID"));
        }
        postJson("/api/requests/" + id + "/verify", Map.of("otp", bad), "op1")
                .andExpect(jsonPath("$.code").value("OTP_LOCKED"));
        // Kể cả OTP đúng cũng không dùng được nữa
        postJson("/api/requests/" + id + "/verify", Map.of("otp", lastEmail().otp()), "op1")
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select status from license_request where id = ?", String.class, id))
                .isEqualTo("FAILED");
        // slot được giải phóng
        createRequest(mm.getId(), 500_000, 300_000, "op1").andExpect(status().isOk());
    }

    @Test
    void otherOperatorCannotVerify() throws Exception {
        long id = body(createRequest(mm.getId(), 500_000, 300_000, "op1")).get("id").asLong();
        postJson("/api/requests/" + id + "/verify", Map.of("otp", lastEmail().otp()), "op2")
                .andExpect(status().isForbidden());
        postJson("/api/requests/" + id + "/verify", Map.of("otp", lastEmail().otp()), "viewer")
                .andExpect(status().isForbidden());
        postJson("/api/requests/" + id + "/verify", Map.of("otp", lastEmail().otp()), "admin")
                .andExpect(status().isOk());
    }

    @Test
    void resendCooldownAndLimit() throws Exception {
        long id = body(createRequest(mm.getId(), 500_000, 300_000, "op1")).get("id").asLong();
        String firstOtp = lastEmail().otp();

        postJson("/api/requests/" + id + "/resend-otp", Map.of(), "op1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RESEND_COOLDOWN"))
                .andExpect(header().exists("Retry-After"));

        for (int i = 0; i < 3; i++) {
            clock.advance(Duration.ofSeconds(61));
            postJson("/api/requests/" + id + "/resend-otp", Map.of(), "op1").andExpect(status().isOk());
        }
        clock.advance(Duration.ofSeconds(61));
        postJson("/api/requests/" + id + "/resend-otp", Map.of(), "op1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RESEND_LIMIT"));

        OtpEmail last = lastEmail();
        assertThat(last.resend()).isTrue();
        // OTP cũ mất hiệu lực sau khi gửi lại
        if (!firstOtp.equals(last.otp())) {
            postJson("/api/requests/" + id + "/verify", Map.of("otp", firstOtp), "op1")
                    .andExpect(jsonPath("$.code").value("OTP_INVALID"));
        }
        postJson("/api/requests/" + id + "/verify", Map.of("otp", last.otp()), "op1").andExpect(status().isOk());
    }

    @Test
    void expiredOtpIsRejectedAndSlotFreed() throws Exception {
        long id = body(createRequest(mm.getId(), 500_000, 300_000, "op1")).get("id").asLong();
        String otp = lastEmail().otp();
        clock.advance(Duration.ofMinutes(31));
        postJson("/api/requests/" + id + "/verify", Map.of("otp", otp), "op1")
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
        assertThat(jdbc.queryForObject("select status from license_request where id = ?", String.class, id))
                .isEqualTo("EXPIRED");
        createRequest(mm.getId(), 500_000, 300_000, "op1").andExpect(status().isOk());
    }

    @Test
    void overduePendingDoesNotBlockNewRequest() throws Exception {
        long id = body(createRequest(mm.getId(), 500_000, 300_000, "op1")).get("id").asLong();
        clock.advance(Duration.ofMinutes(30));
        createRequest(mm.getId(), 600_000, 300_000, "op2").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select status from license_request where id = ?", String.class, id))
                .isEqualTo("EXPIRED");
    }

    @Test
    void expiryJobClosesOverdueRequests() throws Exception {
        createRequest(mm.getId(), 500_000, 300_000, "op1");
        createRequest(th.getId(), 500_000, 300_000, "op1");
        assertThat(service.expireOverdue()).isZero();
        clock.advance(Duration.ofMinutes(30));
        assertThat(service.expireOverdue()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from license_request where pending_market_id is not null",
                Integer.class)).isZero();
    }

    @Test
    void perUserHourlyEmailQuota() throws Exception {
        // max-emails-per-hour-per-user = 10 (mặc định): tạo + hủy liên tục để tiêu hết hạn mức
        for (int i = 0; i < 10; i++) {
            long id = body(createRequest(mm.getId(), 500_000 + i, 300_000, "op1").andExpect(status().isOk()))
                    .get("id").asLong();
            postJson("/api/requests/" + id + "/cancel", Map.of(), "op1").andExpect(status().isOk());
        }
        createRequest(mm.getId(), 900_000, 300_000, "op1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_QUOTA_USER"))
                .andExpect(header().exists("Retry-After"));
        // người khác vẫn tạo được
        createRequest(mm.getId(), 900_000, 300_000, "op2").andExpect(status().isOk());
        // sau 1 giờ hạn mức được hồi lại
        postJson("/api/requests/" + body(mvc.perform(as(get("/api/requests?status=PENDING_OTP"), "op2")))
                .get("content").get(0).get("id").asLong() + "/cancel", Map.of(), "op2");
        clock.advance(Duration.ofMinutes(61));
        createRequest(mm.getId(), 900_000, 300_000, "op1").andExpect(status().isOk());
    }

    @Test
    void validation() throws Exception {
        createRequest(mm.getId(), 100, 200, "op1")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_LIMITS"));
        createRequest(vn.getId(), 100, 50, "op1")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MARKET_NO_LICENSE"));
        createRequest(mm.getId(), 100, 50, "viewer").andExpect(status().isForbidden());
        postJson("/api/requests", Map.of("marketId", mm.getId(), "maxSub", 100, "maxActiveSub", 50, "reason", " "),
                "op1").andExpect(status().isBadRequest());
        verify(notifier, never()).send(any());

        long id = body(createRequest(mm.getId(), 500, 300, "op1")).get("id").asLong();
        postJson("/api/requests/" + id + "/verify", Map.of("otp", lastEmail().otp()), "op1");
        createRequest(mm.getId(), 500, 300, "op1")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NO_CHANGE"));
    }

    @Test
    void mailFailureRollsBackRequest() throws Exception {
        doThrow(new RuntimeException("smtp down")).when(notifier).send(any());
        createRequest(mm.getId(), 500_000, 300_000, "op1")
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("MAIL_FAILED"));
        assertThat(jdbc.queryForObject("select count(*) from license_request", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from audit_log where action = 'OTP_SENT'", Integer.class))
                .isZero();
    }
}
