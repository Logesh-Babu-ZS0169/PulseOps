package com.intics.metrics.repository;

import com.intics.metrics.entity.MFASecret;
import com.intics.metrics.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MFASecretRepository extends JpaRepository<MFASecret, Long> {
    Optional<MFASecret> findByUser(User user);
    Optional<MFASecret> findByUserAndIsEnabled(User user, Boolean isEnabled);
    boolean existsByUser(User user);
}
