package com.vht.license.request;

import com.vht.license.audit.AuditService;
import com.vht.license.common.BusinessException;
import com.vht.license.common.PageDto;
import com.vht.license.common.RetryableBusinessException;
import com.vht.license.config.AppProperties;
import com.vht.license.crypto.LicenseCryptoProvider;
import com.vht.license.crypto.LicensePayload;
import com.vht.license.license.LicenseDto;
import com.vht.license.license.LicenseFile;
import com.vht.license.license.LicenseFileRepository;
import com.vht.license.market.Market;
import com.vht.license.market.MarketRepository;
import com.vht.license.otp.OtpEmail;
import com.vht.license.otp.OtpNotifier;
import com.vht.license.otp.OtpService;
import com.vht.license.otp.OtpService.OtpBinding;
import com.vht.license.request.RequestDtos.CreateRequest;
import com.vht.license.request.RequestDtos.PrevLicenseDto;
import com.vht.license.request.RequestDtos.RequestDto;
import com.vht.license.user.AppUser;
import com.vht.license.user.CurrentUser;
import com.vht.license.user.Role;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Luồng cấp license: tạo yêu cầu + gửi OTP cho người duyệt → người tạo nhập OTP → sinh file.
 *
 * <p>Chống spam OTP:
 * <ul>
 *   <li>Mỗi thị trường tối đa 1 yêu cầu PENDING_OTP (khóa dòng market + cột UNIQUE pending_market_id).</li>
 *   <li>Gửi lại OTP: cách nhau tối thiểu {@code resend-cooldown}, tối đa {@code max-resends} lần mỗi yêu cầu.</li>
 *   <li>Nhập sai tối đa {@code max-attempts} lần mỗi yêu cầu (tính cộng dồn qua các lần gửi lại).</li>
 *   <li>Trần số email OTP mỗi giờ cho toàn hệ thống và cho từng người dùng.</li>
 * </ul>
 */
@Service
public class LicenseRequestService {

    private static final Logger log = LoggerFactory.getLogger(LicenseRequestService.class);
    private static final String OTP_SENT = "OTP_SENT";
    private static final Duration QUOTA_WINDOW = Duration.ofHours(1);
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final LicenseRequestRepository requests;
    private final MarketRepository markets;
    private final LicenseFileRepository licenses;
    private final OtpService otpService;
    private final OtpNotifier notifier;
    private final LicenseCryptoProvider crypto;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final AppProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public LicenseRequestService(LicenseRequestRepository requests, MarketRepository markets,
                                 LicenseFileRepository licenses, OtpService otpService, OtpNotifier notifier,
                                 LicenseCryptoProvider crypto, CurrentUser currentUser, AuditService audit,
                                 AppProperties props, Clock clock) {
        this.requests = requests;
        this.markets = markets;
        this.licenses = licenses;
        this.otpService = otpService;
        this.notifier = notifier;
        this.crypto = crypto;
        this.currentUser = currentUser;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ tạo yêu cầu

    @Transactional
    public RequestDto create(CreateRequest req) {
        AppUser user = currentUser.get();
        Instant now = clock.instant();

        Market market = markets.lockById(req.marketId())
                .orElseThrow(() -> BusinessException.notFound("thị trường"));
        if (!market.isLicenseRequired()) {
            throw BusinessException.badRequest("MARKET_NO_LICENSE", "Thị trường này không quản lý license");
        }
        if (!market.isActive()) {
            throw BusinessException.badRequest("MARKET_INACTIVE", "Thị trường đang ngừng hoạt động");
        }
        if (req.maxActiveSub() > req.maxSub()) {
            throw BusinessException.badRequest("INVALID_LIMITS", "Max Active Sub không được lớn hơn Max Sub");
        }

        Optional<LicenseRequest> existing = requests.findByPendingMarketId(market.getId());
        if (existing.isPresent()) {
            LicenseRequest pending = existing.get();
            if (!pending.isOtpExpired(now)) {
                throw new BusinessException(HttpStatus.CONFLICT, "PENDING_EXISTS",
                        "Thị trường đang có yêu cầu " + pending.getRequestCode() + " của "
                                + pending.getRequestedBy().getDisplayName()
                                + " chờ OTP. Hãy hoàn tất hoặc hủy yêu cầu đó trước.");
            }
            pending.close(RequestStatus.EXPIRED, now, "system");
            // flush ngay để giải phóng pending_market_id trước khi INSERT yêu cầu mới
            requests.saveAndFlush(pending);
        }

        LicenseFile current = licenses.findByMarketIdAndCurrentTrue(market.getId()).orElse(null);
        if (current != null && current.getMaxSub() == req.maxSub() && current.getMaxActiveSub() == req.maxActiveSub()) {
            throw BusinessException.badRequest("NO_CHANGE", "Hai con số giống hệt license hiện hành (v"
                    + current.getVersion() + ")");
        }

        checkEmailQuota(user);

        LicenseRequest request = new LicenseRequest(newRequestCode(now), market, current, req.maxSub(),
                req.maxActiveSub(), req.reason().trim(), user, props.approver().email(), now);
        requests.saveAndFlush(request);

        String otp = otpService.generate();
        request.issueOtp(otpService.hash(binding(request), otp), now, props.otp().ttl());
        audit.log("REQUEST_CREATED", "REQUEST", request.getId(), "code=" + request.getRequestCode()
                + ", market=" + market.getCode() + ", maxSub=" + req.maxSub() + ", maxActiveSub=" + req.maxActiveSub());
        sendOtp(request, otp, false);
        return toDto(request, user);
    }

    // ------------------------------------------------------------------ gửi lại OTP

    @Transactional(noRollbackFor = OtpRejectedException.class)
    public RequestDto resendOtp(Long id) {
        AppUser user = currentUser.get();
        Instant now = clock.instant();
        LicenseRequest request = lockForAction(id, user);
        ensureNotExpired(request, now);

        AppProperties.Otp otpProps = props.otp();
        if (request.getOtpResendCount() >= otpProps.maxResends()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_LIMIT",
                    "Đã gửi lại OTP tối đa " + otpProps.maxResends() + " lần cho yêu cầu này");
        }
        Instant nextAllowed = request.getOtpLastSentAt().plus(otpProps.resendCooldown());
        if (now.isBefore(nextAllowed)) {
            long wait = Duration.between(now, nextAllowed).toSeconds() + 1;
            throw new RetryableBusinessException("RESEND_COOLDOWN",
                    "Vui lòng chờ " + wait + " giây trước khi gửi lại OTP", wait);
        }
        checkEmailQuota(user);

        String otp = otpService.generate();
        request.incrementResend();
        request.issueOtp(otpService.hash(binding(request), otp), now, otpProps.ttl());
        sendOtp(request, otp, true);
        return toDto(request, user);
    }

    // ------------------------------------------------------------------ nhập OTP & sinh file

    @Transactional(noRollbackFor = OtpRejectedException.class)
    public LicenseDto verifyAndGenerate(Long id, String otp) {
        AppUser user = currentUser.get();
        Instant now = clock.instant();
        LicenseRequest request = lockForAction(id, user);
        ensureNotExpired(request, now);

        if (!otpService.matches(binding(request), otp, request.getOtpHash())) {
            int attempts = request.incrementAttempt();
            int left = props.otp().maxAttempts() - attempts;
            if (left <= 0) {
                request.close(RequestStatus.FAILED, now, "system");
                audit.log("OTP_LOCKED", "REQUEST", request.getId(), "code=" + request.getRequestCode());
                throw new OtpRejectedException(HttpStatus.BAD_REQUEST, "OTP_LOCKED",
                        "Nhập sai OTP quá " + props.otp().maxAttempts() + " lần. Yêu cầu đã bị khóa, hãy tạo yêu cầu mới.");
            }
            audit.log("OTP_INVALID", "REQUEST", request.getId(), "attempt=" + attempts);
            throw new OtpRejectedException(HttpStatus.BAD_REQUEST, "OTP_INVALID",
                    "OTP không đúng. Còn " + left + " lần thử.");
        }

        LicenseFile file = generateLicense(request, user, now);
        request.close(RequestStatus.GENERATED, now, user.getUsername());
        audit.log("LICENSE_GENERATED", "LICENSE", file.getId(), "licenseCode=" + file.getLicenseCode()
                + ", request=" + request.getRequestCode() + ", maxSub=" + file.getMaxSub()
                + ", maxActiveSub=" + file.getMaxActiveSub() + ", sha256=" + file.getFileSha256());
        return LicenseDto.of(file);
    }

    // ------------------------------------------------------------------ hủy

    @Transactional
    public RequestDto cancel(Long id) {
        AppUser user = currentUser.get();
        LicenseRequest request = lockForAction(id, user);
        request.close(RequestStatus.CANCELLED, clock.instant(), user.getUsername());
        audit.log("REQUEST_CANCELLED", "REQUEST", request.getId(), "code=" + request.getRequestCode());
        return toDto(request, user);
    }

    // ------------------------------------------------------------------ tra cứu

    @Transactional(readOnly = true)
    public PageDto<RequestDto> search(RequestStatus status, Long marketId, int page, int size) {
        AppUser user = currentUser.get();
        Specification<LicenseRequest> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (marketId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("market").get("id"), marketId));
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageDto.of(requests.findAll(spec, pageable), r -> toDto(r, user));
    }

    @Transactional(readOnly = true)
    public RequestDto get(Long id) {
        return toDto(requests.findById(id).orElseThrow(() -> BusinessException.notFound("yêu cầu")), currentUser.get());
    }

    /** Job định kỳ: đóng các yêu cầu quá hạn để giải phóng slot pending. */
    @Transactional
    public int expireOverdue() {
        return requests.expireOverdue(clock.instant());
    }

    // ------------------------------------------------------------------ nội bộ

    /** Khóa market rồi khóa request (thứ tự cố định để tránh deadlock), kiểm tra quyền và trạng thái. */
    private LicenseRequest lockForAction(Long id, AppUser user) {
        Long marketId = requests.findMarketIdById(id).orElseThrow(() -> BusinessException.notFound("yêu cầu"));
        markets.lockById(marketId);
        LicenseRequest request = requests.lockById(id).orElseThrow(() -> BusinessException.notFound("yêu cầu"));
        if (!canAct(request, user)) {
            throw BusinessException.forbidden("Chỉ người tạo yêu cầu hoặc ADMIN được thực hiện thao tác này");
        }
        if (!request.isPending()) {
            throw new BusinessException(HttpStatus.CONFLICT, "REQUEST_NOT_PENDING",
                    "Yêu cầu không còn ở trạng thái chờ OTP (" + request.getStatus() + ")");
        }
        return request;
    }

    private void ensureNotExpired(LicenseRequest request, Instant now) {
        if (request.isOtpExpired(now)) {
            request.close(RequestStatus.EXPIRED, now, "system");
            throw new OtpRejectedException(HttpStatus.GONE, "OTP_EXPIRED",
                    "OTP đã hết hạn. Yêu cầu đã đóng, hãy tạo yêu cầu mới.");
        }
    }

    private void checkEmailQuota(AppUser user) {
        Instant now = clock.instant();
        Instant since = now.minus(QUOTA_WINDOW);
        AppProperties.Otp otpProps = props.otp();
        if (audit.countSince(OTP_SENT, since) >= otpProps.maxEmailsPerHour()) {
            log.warn("Chạm trần email OTP toàn hệ thống ({}/giờ)", otpProps.maxEmailsPerHour());
            throw new RetryableBusinessException("OTP_QUOTA_GLOBAL",
                    "Hệ thống đã gửi quá nhiều OTP trong 1 giờ qua, vui lòng thử lại sau",
                    retryAfter(null, since, now));
        }
        if (audit.countSince(OTP_SENT, user.getUsername(), since) >= otpProps.maxEmailsPerHourPerUser()) {
            throw new RetryableBusinessException("OTP_QUOTA_USER",
                    "Bạn đã yêu cầu quá " + otpProps.maxEmailsPerHourPerUser() + " OTP trong 1 giờ qua, vui lòng thử lại sau",
                    retryAfter(user.getUsername(), since, now));
        }
    }

    private long retryAfter(String username, Instant since, Instant now) {
        return audit.oldestSince(OTP_SENT, username, since)
                .map(oldest -> Duration.between(now, oldest.plus(QUOTA_WINDOW)).toSeconds() + 1)
                .orElse(60L);
    }

    private void sendOtp(LicenseRequest request, String otp, boolean resend) {
        LicenseFile prev = request.getPrevLicense();
        Market market = request.getMarket();
        OtpEmail email = new OtpEmail(request.getApproverEmail(), request.getRequestCode(), market.getCode(),
                market.getName(), request.getRequestedBy().getDisplayName(), request.getReason(),
                prev == null ? null : prev.getVersion(), prev == null ? null : prev.getMaxSub(),
                prev == null ? null : prev.getMaxActiveSub(), request.getMaxSub(), request.getMaxActiveSub(), otp,
                request.getOtpExpiresAt(), resend);
        // Ghi audit trước khi gửi: nếu gửi lỗi thì cả transaction rollback, không tính vào trần.
        audit.log(OTP_SENT, "REQUEST", request.getId(), "to=" + request.getApproverEmail() + ", resend=" + resend);
        try {
            notifier.send(email);
        } catch (RuntimeException e) {
            log.error("Gửi email OTP thất bại cho {}", request.getRequestCode(), e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "MAIL_FAILED",
                    "Không gửi được email OTP tới người duyệt, vui lòng thử lại sau");
        }
    }

    private LicenseFile generateLicense(LicenseRequest request, AppUser user, Instant now) {
        Market market = request.getMarket();
        licenses.findByMarketIdAndCurrentTrue(market.getId()).ifPresent(prev -> prev.setCurrent(false));
        int version = licenses.maxVersion(market.getId()) + 1;
        var localNow = now.atZone(props.timezone());
        String licenseCode = market.getCode() + "-V" + version + "-" + DAY.format(localNow);
        LicensePayload payload = new LicensePayload(market.getCode(), request.getMaxSub(), request.getMaxActiveSub(),
                licenseCode, version, now);
        byte[] content = crypto.encrypt(payload);
        String fileName = "license_" + market.getCode() + "_v" + version + "_" + MINUTE.format(localNow) + "."
                + crypto.fileExtension();
        return licenses.save(new LicenseFile(licenseCode, market, request, version, request.getMaxSub(),
                request.getMaxActiveSub(), fileName, content, sha256(content), crypto.name(), user, now));
    }

    private static OtpBinding binding(LicenseRequest r) {
        return new OtpBinding(r.getId(), r.getMarket().getCode(), r.getMaxSub(), r.getMaxActiveSub());
    }

    private String newRequestCode(Instant now) {
        StringBuilder sb = new StringBuilder("REQ-").append(DAY.format(now.atZone(props.timezone()))).append('-');
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean canAct(LicenseRequest r, AppUser user) {
        return user.getRole() == Role.ADMIN
                || (user.getRole() == Role.OPERATOR && r.getRequestedBy().getId().equals(user.getId()));
    }

    private RequestDto toDto(LicenseRequest r, AppUser user) {
        LicenseFile prev = r.getPrevLicense();
        AppProperties.Otp otpProps = props.otp();
        boolean pending = r.isPending();
        Long licenseId = r.getStatus() == RequestStatus.GENERATED
                ? licenses.findByRequestId(r.getId()).map(LicenseFile::getId).orElse(null) : null;
        return new RequestDto(r.getId(), r.getRequestCode(), r.getMarket().getId(), r.getMarket().getCode(),
                r.getMarket().getName(),
                prev == null ? null : new PrevLicenseDto(prev.getId(), prev.getVersion(), prev.getMaxSub(),
                        prev.getMaxActiveSub()),
                r.getMaxSub(), r.getMaxActiveSub(), r.getReason(), r.getStatus(),
                r.getRequestedBy().getDisplayName(), r.getRequestedBy().getUsername(), r.getApproverEmail(),
                pending ? r.getOtpExpiresAt() : null, r.getOtpLastSentAt(),
                pending && r.getOtpLastSentAt() != null ? r.getOtpLastSentAt().plus(otpProps.resendCooldown()) : null,
                pending ? Math.max(0, otpProps.maxResends() - r.getOtpResendCount()) : 0,
                pending ? Math.max(0, otpProps.maxAttempts() - r.getOtpAttemptCount()) : 0,
                r.getCreatedAt(), r.getClosedAt(), r.getClosedBy(), licenseId, pending && canAct(r, user));
    }
}
