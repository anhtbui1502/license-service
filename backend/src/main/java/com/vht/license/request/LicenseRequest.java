package com.vht.license.request;

import com.vht.license.license.LicenseFile;
import com.vht.license.market.Market;
import com.vht.license.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "license_request")
public class LicenseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_code", nullable = false, unique = true, length = 40)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id")
    private Market market;

    /** License hiện hành tại thời điểm tạo yêu cầu (null nếu thị trường chưa có license). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prev_license_id")
    private LicenseFile prevLicense;

    @Column(name = "max_sub", nullable = false)
    private long maxSub;

    @Column(name = "max_active_sub", nullable = false)
    private long maxActiveSub;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    /** = market_id khi PENDING_OTP, null khi khác; cột UNIQUE nên mỗi thị trường chỉ có 1 yêu cầu chờ. */
    @Column(name = "pending_market_id", unique = true)
    private Long pendingMarketId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    private AppUser requestedBy;

    @Column(name = "approver_email", nullable = false, length = 200)
    private String approverEmail;

    @Column(name = "otp_hash", length = 128)
    private String otpHash;

    @Column(name = "otp_expires_at")
    private Instant otpExpiresAt;

    @Column(name = "otp_last_sent_at")
    private Instant otpLastSentAt;

    @Column(name = "otp_resend_count", nullable = false)
    private int otpResendCount;

    @Column(name = "otp_attempt_count", nullable = false)
    private int otpAttemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    protected LicenseRequest() {
    }

    public LicenseRequest(String requestCode, Market market, LicenseFile prevLicense, long maxSub, long maxActiveSub,
                          String reason, AppUser requestedBy, String approverEmail, Instant createdAt) {
        this.requestCode = requestCode;
        this.market = market;
        this.prevLicense = prevLicense;
        this.maxSub = maxSub;
        this.maxActiveSub = maxActiveSub;
        this.reason = reason;
        this.requestedBy = requestedBy;
        this.approverEmail = approverEmail;
        this.createdAt = createdAt;
        this.status = RequestStatus.PENDING_OTP;
        this.pendingMarketId = market.getId();
    }

    public void issueOtp(String otpHash, Instant now, Duration ttl) {
        this.otpHash = otpHash;
        this.otpLastSentAt = now;
        this.otpExpiresAt = now.plus(ttl);
    }

    public void incrementResend() {
        this.otpResendCount++;
    }

    public int incrementAttempt() {
        return ++this.otpAttemptCount;
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING_OTP;
    }

    public boolean isOtpExpired(Instant now) {
        return otpExpiresAt == null || !now.isBefore(otpExpiresAt);
    }

    /** Chuyển sang trạng thái kết thúc và giải phóng slot pending của thị trường. */
    public void close(RequestStatus newStatus, Instant now, String by) {
        if (newStatus == RequestStatus.PENDING_OTP) {
            throw new IllegalArgumentException("close() cần trạng thái kết thúc");
        }
        this.status = newStatus;
        this.pendingMarketId = null;
        this.otpHash = null;
        this.closedAt = now;
        this.closedBy = by;
    }

    public Long getId() {
        return id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public Market getMarket() {
        return market;
    }

    public LicenseFile getPrevLicense() {
        return prevLicense;
    }

    public long getMaxSub() {
        return maxSub;
    }

    public long getMaxActiveSub() {
        return maxActiveSub;
    }

    public String getReason() {
        return reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public AppUser getRequestedBy() {
        return requestedBy;
    }

    public String getApproverEmail() {
        return approverEmail;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public Instant getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public Instant getOtpLastSentAt() {
        return otpLastSentAt;
    }

    public int getOtpResendCount() {
        return otpResendCount;
    }

    public int getOtpAttemptCount() {
        return otpAttemptCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }
}
