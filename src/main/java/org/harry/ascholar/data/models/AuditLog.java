package org.harry.ascholar.data.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Entity
    @Table(name = "audit_logs", indexes = {
            @Index(name = "idx_audit_user_id", columnList = "user_id"),
            @Index(name = "idx_audit_action", columnList = "action"),
            @Index(name = "idx_audit_timestamp", columnList = "timestamp")
    })
    public class AuditLog {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id")
        private Long userId;

        @Column(nullable = false, length = 100)
        private String action;

        @Column(columnDefinition = "TEXT")
        private String description;

        @Column(name = "ip_address", length = 45)
        private String ipAddress;

        @Column(name = "user_agent", length = 500)
        private String userAgent;

        @Column(nullable = false)
        private LocalDateTime timestamp;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "jsonb")
        private Map<String, String> metadata;
}
