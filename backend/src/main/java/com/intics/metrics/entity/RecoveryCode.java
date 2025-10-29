package com.intics.metrics.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_codes", schema = "intics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isUsed == null) {
            isUsed = false;
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusYears(1);
        }
    }
}
