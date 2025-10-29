package com.intics.metrics.service;

import com.intics.metrics.entity.MFAAuditLog;
import com.intics.metrics.entity.User;
import com.intics.metrics.repository.MFAAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MFAAuditService {

    private final MFAAuditLogRepository auditLogRepository;

    @Transactional
    public void logMFAAction(User user, String action, boolean success, String failureReason, 
                             String resourceType, String resourceId, HttpServletRequest request) {
        MFAAuditLog auditLog = new MFAAuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setSuccess(success);
        auditLog.setFailureReason(failureReason);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        
        if (request != null) {
            auditLog.setIpAddress(getClientIP(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }

        auditLogRepository.save(auditLog);
        
        log.info("MFA Audit Log - User: {}, Action: {}, Success: {}, Resource: {}/{}", 
                user.getUsername(), action, success, resourceType, resourceId);
    }

    @Transactional
    public void logMFAEnrollment(User user, boolean success, HttpServletRequest request) {
        logMFAAction(user, "MFA_ENROLLMENT", success, null, null, null, request);
    }

    @Transactional
    public void logMFAVerification(User user, boolean success, String failureReason, HttpServletRequest request) {
        logMFAAction(user, "MFA_VERIFICATION", success, failureReason, null, null, request);
    }

    @Transactional
    public void logRecoveryCodeUsage(User user, boolean success, String failureReason, HttpServletRequest request) {
        logMFAAction(user, "RECOVERY_CODE_USED", success, failureReason, null, null, request);
    }

    @Transactional
    public void logSecureAccess(User user, String resourceType, String resourceId, boolean success, 
                                String failureReason, HttpServletRequest request) {
        logMFAAction(user, "SECURE_ACCESS", success, failureReason, resourceType, resourceId, request);
    }

    @Transactional
    public void logMFADisabled(User user, HttpServletRequest request) {
        logMFAAction(user, "MFA_DISABLED", true, null, null, null, request);
    }

    public List<MFAAuditLog> getUserAuditLogs(User user) {
        return auditLogRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<MFAAuditLog> getUserAuditLogsSince(User user, LocalDateTime since) {
        return auditLogRepository.findByUserAndCreatedAtAfterOrderByCreatedAtDesc(user, since);
    }

    public boolean hasExceededRateLimit(User user, int maxAttempts, int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        long failedAttempts = auditLogRepository.countByUserAndSuccessAndCreatedAtAfter(user, false, cutoff);
        return failedAttempts >= maxAttempts;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}
