package com.vht.license.audit;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    long countByActionAndCreatedAtAfter(String action, Instant after);

    long countByActionAndUsernameAndCreatedAtAfter(String action, String username, Instant after);

    Optional<AuditLog> findFirstByActionAndCreatedAtAfterOrderByCreatedAtAsc(String action, Instant after);

    Optional<AuditLog> findFirstByActionAndUsernameAndCreatedAtAfterOrderByCreatedAtAsc(String action, String username,
                                                                                         Instant after);
}
