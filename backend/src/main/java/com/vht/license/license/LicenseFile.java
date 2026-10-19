package com.vht.license.license;

import com.vht.license.market.Market;
import com.vht.license.request.LicenseRequest;
import com.vht.license.user.AppUser;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Một file license đã sinh cho thị trường (lịch sử, không xóa). */
@Entity
@Table(name = "license_file")
public class LicenseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_code", nullable = false, unique = true, length = 60)
    private String licenseCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id")
    private Market market;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private LicenseRequest request;

    @Column(nullable = false)
    private int version;

    @Column(name = "max_sub", nullable = false)
    private long maxSub;

    @Column(name = "max_active_sub", nullable = false)
    private long maxActiveSub;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_content", nullable = false)
    private byte[] fileContent;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "crypto_name", nullable = false, length = 50)
    private String cryptoName;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private AppUser issuedBy;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected LicenseFile() {
    }

    public LicenseFile(String licenseCode, Market market, LicenseRequest request, int version, long maxSub,
                       long maxActiveSub, String fileName, byte[] fileContent, String fileSha256, String cryptoName,
                       AppUser issuedBy, Instant issuedAt) {
        this.licenseCode = licenseCode;
        this.market = market;
        this.request = request;
        this.version = version;
        this.maxSub = maxSub;
        this.maxActiveSub = maxActiveSub;
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.fileSha256 = fileSha256;
        this.cryptoName = cryptoName;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
        this.current = true;
    }

    public Long getId() {
        return id;
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public Market getMarket() {
        return market;
    }

    public LicenseRequest getRequest() {
        return request;
    }

    public int getVersion() {
        return version;
    }

    public long getMaxSub() {
        return maxSub;
    }

    public long getMaxActiveSub() {
        return maxActiveSub;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public String getCryptoName() {
        return cryptoName;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public AppUser getIssuedBy() {
        return issuedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
