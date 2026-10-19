package com.vht.license.license;

import com.vht.license.audit.AuditService;
import com.vht.license.common.BusinessException;
import com.vht.license.common.PageDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenseService {

    private final LicenseFileRepository licenses;
    private final AuditService audit;

    public LicenseService(LicenseFileRepository licenses, AuditService audit) {
        this.licenses = licenses;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageDto<LicenseDto> search(Long marketId, int page, int size) {
        Specification<LicenseFile> spec = Specification.where(null);
        if (marketId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("market").get("id"), marketId));
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "issuedAt", "id"));
        return PageDto.of(licenses.findAll(spec, pageable), LicenseDto::of);
    }

    @Transactional(readOnly = true)
    public LicenseDto get(Long id) {
        return LicenseDto.of(find(id));
    }

    public record Download(String fileName, byte[] content, String sha256) {
    }

    @Transactional
    public Download download(Long id) {
        LicenseFile l = find(id);
        audit.log("LICENSE_DOWNLOADED", "LICENSE", l.getId(), "licenseCode=" + l.getLicenseCode());
        return new Download(l.getFileName(), l.getFileContent(), l.getFileSha256());
    }

    private LicenseFile find(Long id) {
        return licenses.findById(id).orElseThrow(() -> BusinessException.notFound("file license"));
    }
}
