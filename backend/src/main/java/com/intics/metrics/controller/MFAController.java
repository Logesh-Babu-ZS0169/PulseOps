package com.intics.metrics.controller;

import com.intics.metrics.dto.*;
import com.intics.metrics.entity.User;
import com.intics.metrics.repository.UserRepository;
import com.intics.metrics.service.MFAAuditService;
import com.intics.metrics.service.MFAService;
import com.intics.metrics.service.MFATokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/multi-factor-auth")
@Tag(name = "Multi-Factor Authentication", description = "Banking-grade MFA for secure database and Kubernetes access")
@RequiredArgsConstructor
@Slf4j
public class MFAController {

    private final MFAService mfaService;
    private final MFAAuditService auditService;
    private final MFATokenService tokenService;
    private final UserRepository userRepository;

    private static final int MAX_ATTEMPTS = 5;
    private static final int RATE_LIMIT_MINUTES = 15;

    @PostMapping("/enroll")
    @Operation(summary = "Enroll in MFA", description = "Generate QR code and secret key for Google Authenticator enrollment")
    public ResponseEntity<?> enrollMFA(Authentication authentication, HttpServletRequest request) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            Map<String, Object> enrollment = mfaService.enrollMFA(user);
            
            MFAEnrollmentResponse response = new MFAEnrollmentResponse();
            response.setSecretKey((String) enrollment.get("secretKey"));
            response.setQrCodeImage((String) enrollment.get("qrCodeImage"));
            response.setQrCodeUrl((String) enrollment.get("qrCodeUrl"));
            response.setIssuer((String) enrollment.get("issuer"));
            response.setUsername((String) enrollment.get("username"));
            response.setMessage("Scan the QR code with Google Authenticator or Authy app, then verify with a code to enable MFA");
            
            auditService.logMFAEnrollment(user, true, request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MFA enrollment failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "MFA enrollment failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/verify-and-enable")
    @Operation(summary = "Verify and enable MFA", description = "Verify TOTP code and enable MFA for the user")
    public ResponseEntity<?> verifyAndEnable(@RequestBody MFAVerificationRequest request, 
                                            Authentication authentication, 
                                            HttpServletRequest httpRequest) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            // Check rate limiting
            if (auditService.hasExceededRateLimit(user, MAX_ATTEMPTS, RATE_LIMIT_MINUTES)) {
                auditService.logMFAVerification(user, false, "Rate limit exceeded", httpRequest);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Too many failed attempts. Please try again later."));
            }
            
            boolean isValid = mfaService.verifyAndEnable(user, request.getCode());
            
            if (isValid) {
                auditService.logMFAVerification(user, true, null, httpRequest);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "MFA enabled successfully! Please generate recovery codes for backup access."
                ));
            } else {
                auditService.logMFAVerification(user, false, "Invalid TOTP code", httpRequest);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            log.error("MFA verification failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "MFA verification failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify MFA code", description = "Verify TOTP code for already enabled MFA")
    public ResponseEntity<?> verify(@RequestBody MFAVerificationRequest request, 
                                   Authentication authentication, 
                                   HttpServletRequest httpRequest) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            // Check rate limiting
            if (auditService.hasExceededRateLimit(user, MAX_ATTEMPTS, RATE_LIMIT_MINUTES)) {
                auditService.logMFAVerification(user, false, "Rate limit exceeded", httpRequest);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Too many failed attempts. Please try again later."));
            }
            
            boolean isValid = mfaService.verify(user, request.getCode());
            
            if (isValid) {
                auditService.logMFAVerification(user, true, null, httpRequest);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "MFA verification successful"
                ));
            } else {
                auditService.logMFAVerification(user, false, "Invalid TOTP code", httpRequest);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            log.error("MFA verification failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "MFA verification failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/challenge")
    @Operation(summary = "Request MFA challenge for resource access", 
               description = "Verify MFA and get temporary session token for database/Kubernetes access")
    public ResponseEntity<?> challenge(@RequestBody Map<String, Object> challengeRequest,
                                      Authentication authentication,
                                      HttpServletRequest httpRequest) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            int code = (Integer) challengeRequest.get("code");
            String resourceType = (String) challengeRequest.get("resourceType");
            String resourceId = (String) challengeRequest.get("resourceId");
            
            // Check rate limiting
            if (auditService.hasExceededRateLimit(user, MAX_ATTEMPTS, RATE_LIMIT_MINUTES)) {
                auditService.logSecureAccess(user, resourceType, resourceId, false, "Rate limit exceeded", httpRequest);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Too many failed attempts. Please try again later."));
            }
            
            boolean isValid = mfaService.verify(user, code);
            
            if (isValid) {
                // Generate MFA session token (valid for 5 minutes)
                String mfaToken = tokenService.generateMFASessionToken(user.getUsername(), resourceType, resourceId);
                
                auditService.logSecureAccess(user, resourceType, resourceId, true, null, httpRequest);
                
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "mfaToken", mfaToken,
                        "expiresIn", 300,
                        "message", "MFA verification successful. Use this token to access the resource."
                ));
            } else {
                auditService.logSecureAccess(user, resourceType, resourceId, false, "Invalid TOTP code", httpRequest);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            log.error("MFA challenge failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-recovery-code")
    @Operation(summary = "Verify recovery code", description = "Use one-time recovery code as alternative to TOTP")
    public ResponseEntity<?> verifyRecoveryCode(@RequestBody RecoveryCodeRequest request,
                                               Authentication authentication,
                                               HttpServletRequest httpRequest) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            boolean isValid = mfaService.verifyRecoveryCode(user, request.getCode());
            
            if (isValid) {
                auditService.logRecoveryCodeUsage(user, true, null, httpRequest);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Recovery code verified successfully"
                ));
            } else {
                auditService.logRecoveryCodeUsage(user, false, "Invalid or used recovery code", httpRequest);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or already used recovery code"));
            }
        } catch (Exception e) {
            log.error("Recovery code verification failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recovery-codes/generate")
    @Operation(summary = "Generate recovery codes", description = "Generate 10 one-time use recovery codes")
    public ResponseEntity<?> generateRecoveryCodes(Authentication authentication, HttpServletRequest request) {
        try {
            User user = getUserFromAuthentication(authentication);
            
            if (!mfaService.isMFAEnabled(user)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "MFA must be enabled before generating recovery codes"));
            }
            
            List<String> codes = mfaService.generateRecoveryCodes(user);
            
            RecoveryCodesResponse response = new RecoveryCodesResponse();
            response.setCodes(codes);
            response.setTotalCodes(codes.size());
            response.setMessage("Save these recovery codes in a secure location. Each code can only be used once.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Recovery code generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Get MFA status", description = "Check if MFA is enabled and get usage information")
    public ResponseEntity<?> getMFAStatus(Authentication authentication) {
        try {
            User user = getUserFromAuthentication(authentication);
            Map<String, Object> status = mfaService.getMFAStatus(user);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get MFA status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable MFA", description = "Disable MFA for the user")
    public ResponseEntity<?> disableMFA(Authentication authentication, HttpServletRequest request) {
        try {
            User user = getUserFromAuthentication(authentication);
            mfaService.disableMFA(user);
            auditService.logMFADisabled(user, request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MFA has been disabled"
            ));
        } catch (Exception e) {
            log.error("Failed to disable MFA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Authentication required. Please login first and provide a valid JWT token.");
        }
        
        // The JWT filter sets the User object as the principal
        if (authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        
        // Fallback: lookup by username
        String username = authentication.getName();
        log.info("Username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
