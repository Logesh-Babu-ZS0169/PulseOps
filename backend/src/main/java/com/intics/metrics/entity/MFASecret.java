package com.intics.metrics.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mfa_secrets", schema = "intics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MFASecret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "secret_key", nullable = false, length = 500)
    private String secretKey;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "backup_codes_generated")
    private Boolean backupCodesGenerated;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isEnabled == null) {
            isEnabled = false;
        }
        if (backupCodesGenerated == null) {
            backupCodesGenerated = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
