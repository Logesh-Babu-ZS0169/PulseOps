package com.intics.metrics.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_config", schema = "inbound_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_key", unique = true, nullable = false, length = 100)
    private String configKey;
    
    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "is_sensitive", nullable = false)
    private Boolean isSensitive = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
