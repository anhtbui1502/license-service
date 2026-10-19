package com.vht.license.otp;

import static org.assertj.core.api.Assertions.assertThat;

import com.vht.license.config.AppProperties;
import com.vht.license.otp.OtpService.OtpBinding;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class OtpServiceTest {

    static final AppProperties PROPS = new AppProperties(ZoneId.of("Asia/Ho_Chi_Minh"),
            new AppProperties.Approver("d@x.vn", "Director"), new AppProperties.Mail("lm@x.vn"),
            new AppProperties.Otp(Duration.ofMinutes(30), Duration.ofSeconds(60), 3, 5, 30, 10,
                    "0123456789abcdef0123456789abcdef"),
            new AppProperties.License("mock"), new AppProperties.Bootstrap(null, null, null));

    final OtpService service = new OtpService(PROPS);

    @Test
    void otpIsBoundToRequestContent() {
        String otp = service.generate();
        assertThat(otp).matches("\\d{6}");
        OtpBinding b = new OtpBinding(1, "MM", 500_000, 300_000);
        String hash = service.hash(b, otp);
        assertThat(service.matches(b, otp, hash)).isTrue();
        assertThat(service.matches(new OtpBinding(2, "MM", 500_000, 300_000), otp, hash)).isFalse();
        assertThat(service.matches(new OtpBinding(1, "TH", 500_000, 300_000), otp, hash)).isFalse();
        assertThat(service.matches(new OtpBinding(1, "MM", 900_000, 300_000), otp, hash)).isFalse();
        assertThat(service.matches(new OtpBinding(1, "MM", 500_000, 400_000), otp, hash)).isFalse();
        assertThat(service.matches(b, null, hash)).isFalse();
    }

    @Test
    void emailBodyContainsNumbersAndEscapesHtml() {
        SmtpOtpNotifier n = new SmtpOtpNotifier(null, PROPS);
        OtpEmail e = new OtpEmail("d@x.vn", "REQ-1", "MM", "Myanmar", "Op <b>1</b>", "PO-1 <script>",
                3, 500_000L, 300_000L, 700_000, 250_000, "482913", Instant.parse("2026-10-19T03:30:00Z"), false);
        String body = n.body(e);
        assertThat(body).contains("482913", "700.000", "+200.000", "-50.000", "10:30 19/10/2026", "Hiện tại (v3)")
                .doesNotContain("<script>").contains("&lt;script&gt;");
        assertThat(n.subject(e)).contains("REQ-1", "Myanmar", "700.000", "250.000");
        assertThat(e.toString()).doesNotContain("482913");
    }
}
