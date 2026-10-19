package com.vht.license.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Nhật ký thao tác, chỉ ghi thêm, không sửa/xóa. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 4000)
    private String detail;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(String username, String action, String entityType, Long entityId, String detail,
                    String ipAddress, Instant createdAt) {
        this.username = username;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.detail = detail != null && detail.length() > 4000 ? detail.substring(0, 4000) : detail;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
