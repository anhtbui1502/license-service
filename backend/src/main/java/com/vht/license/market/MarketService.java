package com.vht.license.market;

import com.vht.license.audit.AuditService;
import com.vht.license.common.BusinessException;
import com.vht.license.license.LicenseFile;
import com.vht.license.license.LicenseFileRepository;
import com.vht.license.market.MarketDtos.CreateMarketRequest;
import com.vht.license.market.MarketDtos.CurrentLicenseDto;
import com.vht.license.market.MarketDtos.MarketDto;
import com.vht.license.market.MarketDtos.PendingRequestDto;
import com.vht.license.market.MarketDtos.UpdateMarketRequest;
import com.vht.license.request.LicenseRequest;
import com.vht.license.request.LicenseRequestRepository;
import com.vht.license.request.RequestStatus;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketService {

    private final MarketRepository markets;
    private final LicenseFileRepository licenses;
    private final LicenseRequestRepository requests;
    private final AuditService audit;
    private final Clock clock;

    public MarketService(MarketRepository markets, LicenseFileRepository licenses, LicenseRequestRepository requests,
                         AuditService audit, Clock clock) {
        this.markets = markets;
        this.licenses = licenses;
        this.requests = requests;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MarketDto> list() {
        Map<Long, LicenseFile> current = licenses.findByCurrentTrue().stream()
                .collect(Collectors.toMap(l -> l.getMarket().getId(), Function.identity()));
        Map<Long, LicenseRequest> pending = requests.findByStatus(RequestStatus.PENDING_OTP).stream()
                .collect(Collectors.toMap(r -> r.getMarket().getId(), Function.identity()));
        return markets.findAll(Sort.by("code")).stream()
                .map(m -> toDto(m, current.get(m.getId()), pending.get(m.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MarketDto get(Long id) {
        Market m = markets.findById(id).orElseThrow(() -> BusinessException.notFound("thị trường"));
        return toDto(m, licenses.findByMarketIdAndCurrentTrue(id).orElse(null),
                requests.findByPendingMarketId(id).orElse(null));
    }

    @Transactional
    public MarketDto create(CreateMarketRequest req) {
        if (markets.existsByCode(req.code())) {
            throw new BusinessException(HttpStatus.CONFLICT, "MARKET_CODE_EXISTS", "Mã thị trường đã tồn tại");
        }
        Market m = new Market(req.code(), req.name(), clock.instant());
        m.setCountry(req.country());
        m.setLicenseRequired(req.licenseRequired());
        m.setNote(req.note());
        markets.save(m);
        audit.log("MARKET_CREATED", "MARKET", m.getId(), "code=" + m.getCode() + ", licenseRequired=" + m.isLicenseRequired());
        return toDto(m, null, null);
    }

    @Transactional
    public MarketDto update(Long id, UpdateMarketRequest req) {
        Market m = markets.lockById(id).orElseThrow(() -> BusinessException.notFound("thị trường"));
        m.setName(req.name());
        m.setCountry(req.country());
        m.setLicenseRequired(req.licenseRequired());
        m.setActive(req.active());
        m.setNote(req.note());
        m.setUpdatedAt(clock.instant());
        audit.log("MARKET_UPDATED", "MARKET", m.getId(), "code=" + m.getCode() + ", licenseRequired="
                + m.isLicenseRequired() + ", active=" + m.isActive());
        return get(id);
    }

    private static MarketDto toDto(Market m, LicenseFile current, LicenseRequest pending) {
        CurrentLicenseDto cur = current == null ? null : new CurrentLicenseDto(current.getId(),
                current.getLicenseCode(), current.getVersion(), current.getMaxSub(), current.getMaxActiveSub(),
                current.getIssuedAt());
        PendingRequestDto pen = pending == null ? null : new PendingRequestDto(pending.getId(),
                pending.getRequestCode(), pending.getMaxSub(), pending.getMaxActiveSub(),
                pending.getRequestedBy().getDisplayName(), pending.getOtpExpiresAt());
        return new MarketDto(m.getId(), m.getCode(), m.getName(), m.getCountry(), m.isLicenseRequired(),
                m.isActive(), m.getNote(), m.getCreatedAt(), cur, pen);
    }
}
