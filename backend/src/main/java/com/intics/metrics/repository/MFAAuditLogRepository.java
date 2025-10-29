package com.intics.metrics.repository;

import com.intics.metrics.entity.MFAAuditLog;
import com.intics.metrics.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MFAAuditLogRepository extends JpaRepository<MFAAuditLog, Long> {
    List<MFAAuditLog> findByUserOrderByCreatedAtDesc(User user);
    List<MFAAuditLog> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, LocalDateTime after);
    long countByUserAndSuccessAndCreatedAtAfter(User user, Boolean success, LocalDateTime after);
}
