package com.vht.license;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vht.license.market.Market;
import com.vht.license.market.MarketRepository;
import com.vht.license.otp.OtpEmail;
import com.vht.license.otp.OtpNotifier;
import com.vht.license.user.AppUser;
import com.vht.license.user.AppUserRepository;
import com.vht.license.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.TestClockConfig.class)
public abstract class IntegrationTestBase {

    protected static final Instant START = Instant.parse("2026-10-19T03:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    public static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START);
        }
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected MutableClock clock;
    @Autowired
    protected AppUserRepository users;
    @Autowired
    protected MarketRepository markets;
    @MockBean
    protected OtpNotifier notifier;

    protected Market mm;
    protected Market th;
    protected Market vn;

    @BeforeEach
    void resetData() {
        jdbc.update("update license_request set prev_license_id = null");
        jdbc.update("delete from license_file");
        jdbc.update("delete from license_request");
        jdbc.update("delete from market");
        jdbc.update("delete from audit_log");
        jdbc.update("delete from app_user");
        Mockito.reset(notifier);
        clock.set(START);

        for (var u : List.of(new Object[]{"admin", Role.ADMIN}, new Object[]{"op1", Role.OPERATOR},
                new Object[]{"op2", Role.OPERATOR}, new Object[]{"viewer", Role.VIEWER})) {
            users.save(new AppUser((String) u[0], null, u[0] + "@test.local", (Role) u[1], "{noop}x", START));
        }
        mm = markets.save(new Market("MM", "Myanmar", START));
        th = markets.save(new Market("TH", "Thailand", START));
        Market v = new Market("VN", "Việt Nam", START);
        v.setLicenseRequired(false);
        vn = markets.save(v);
    }

    protected Clock clock() {
        return clock;
    }

    protected MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, String username) {
        AppUser u = users.findByUsername(username).orElseThrow();
        return b.with(user(username).roles(u.getRole().name())).with(csrf());
    }

    protected ResultActions postJson(String url, Object body, String username) throws Exception {
        return mvc.perform(as(post(url), username).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    protected ResultActions createRequest(Long marketId, long maxSub, long maxActiveSub, String username)
            throws Exception {
        return postJson("/api/requests", Map.of("marketId", marketId, "maxSub", maxSub,
                "maxActiveSub", maxActiveSub, "reason", "Mở rộng theo PO-001"), username);
    }

    protected JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    /** OTP trong email gửi gần nhất. */
    protected OtpEmail lastEmail() {
        ArgumentCaptor<OtpEmail> captor = ArgumentCaptor.forClass(OtpEmail.class);
        verify(notifier, atLeastOnce()).send(captor.capture());
        List<OtpEmail> all = captor.getAllValues();
        return all.get(all.size() - 1);
    }

    protected static <T> T anyArg() {
        return any();
    }
}
