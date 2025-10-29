package com.intics.metrics.repository;

import com.intics.metrics.entity.RecoveryCode;
import com.intics.metrics.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {
    List<RecoveryCode> findByUserAndIsUsed(User user, Boolean isUsed);
    Optional<RecoveryCode> findByCodeAndIsUsed(String code, Boolean isUsed);
    void deleteByUser(User user);
    long countByUserAndIsUsed(User user, Boolean isUsed);
}
