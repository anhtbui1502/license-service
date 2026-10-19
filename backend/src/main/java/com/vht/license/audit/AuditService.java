package com.vht.license.audit;

import com.vht.license.common.PageDto;
import com.vht.license.common.RequestContext;
import com.vht.license.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Ghi trong transaction hiện tại: thao tác rollback thì log cũng rollback. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void log(String action, String entityType, Long entityId, String detail) {
        repository.save(new AuditLog(CurrentUser.username(), action, entityType, entityId, detail,
                RequestContext.clientIp(), clock.instant()));
    }

    /** Ghi trong transaction riêng, dùng cho sự kiện cần lưu kể cả khi thao tác chính thất bại. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIndependently(String username, String action, String detail) {
        repository.save(new AuditLog(username, action, null, null, detail, RequestContext.clientIp(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public long countSince(String action, Instant since) {
        return repository.countByActionAndCreatedAtAfter(action, since);
    }

    @Transactional(readOnly = true)
    public long countSince(String action, String username, Instant since) {
        return repository.countByActionAndUsernameAndCreatedAtAfter(action, username, since);
    }

    /** Thời điểm sự kiện cũ nhất trong cửa sổ (để tính lúc được thử lại khi chạm trần). */
    @Transactional(readOnly = true)
    public Optional<Instant> oldestSince(String action, String username, Instant since) {
        Optional<AuditLog> log = username == null
                ? repository.findFirstByActionAndCreatedAtAfterOrderByCreatedAtAsc(action, since)
                : repository.findFirstByActionAndUsernameAndCreatedAtAfterOrderByCreatedAtAsc(action, username, since);
        return log.map(AuditLog::getCreatedAt);
    }

    @Transactional(readOnly = true)
    public PageDto<AuditLogDto> search(String action, String username, int page, int size) {
        Specification<AuditLog> spec = Specification.where(null);
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action));
        }
        if (username != null && !username.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("username"), username));
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageDto.of(repository.findAll(spec, pageable), AuditLogDto::of);
    }

    public record AuditLogDto(Long id, String username, String action, String entityType, Long entityId,
                              String detail, String ipAddress, Instant createdAt) {
        static AuditLogDto of(AuditLog a) {
            return new AuditLogDto(a.getId(), a.getUsername(), a.getAction(), a.getEntityType(), a.getEntityId(),
                    a.getDetail(), a.getIpAddress(), a.getCreatedAt());
        }
    }
}
