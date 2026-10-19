package com.vht.license.audit;

import com.vht.license.audit.AuditService.AuditLogDto;
import com.vht.license.common.PageDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public PageDto<AuditLogDto> search(@RequestParam(required = false) String action,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        return auditService.search(action, username, page, size);
    }
}
