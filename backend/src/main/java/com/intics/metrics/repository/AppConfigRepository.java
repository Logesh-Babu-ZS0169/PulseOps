package com.intics.metrics.repository;

import com.intics.metrics.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
    
    Optional<AppConfig> findByConfigKey(String configKey);
    
    List<AppConfig> findByCategory(String category);
    
    boolean existsByConfigKey(String configKey);
}
