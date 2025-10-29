package com.intics.metrics.interceptor;

import com.intics.metrics.annotation.RequiresMFA;
import com.intics.metrics.entity.User;
import com.intics.metrics.repository.UserRepository;
import com.intics.metrics.service.MFAService;
import com.intics.metrics.service.MFATokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class MFAInterceptor implements HandlerInterceptor {

    private final MFAService mfaService;
    private final MFATokenService mfaTokenService;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresMFA requiresMFA = handlerMethod.getMethodAnnotation(RequiresMFA.class);

        if (requiresMFA == null) {
            return true;
        }

        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Authentication required\"}");
            return false;
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof User) {
            username = ((User) principal).getUsername();
        } else {
            username = authentication.getName();
        }

        log.info("✅ Extracted username from principal: {}", username);

        log.info("🔍 Authentication name from security context: '{}'", authentication.getName());
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"User not found\"}");
            return false;
        }

        // Check if MFA is enabled for user
        if (!mfaService.isMFAEnabled(user)) {
            log.warn("MFA not enabled for user accessing protected resource: {}", username);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"MFA must be enabled to access this resource\",\"mfaRequired\":true}");
            return false;
        }

        // Check for MFA token in header
        String mfaToken = request.getHeader("X-MFA-Token");
        if (mfaToken == null || mfaToken.isEmpty()) {
            log.warn("MFA token missing for protected resource access by user: {}", username);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"MFA verification required\",\"mfaChallenge\":true}");
            return false;
        }

        // Validate MFA token
        String resourceType = requiresMFA.resourceType();
        String resourceId = requiresMFA.resourceId().isEmpty() ? request.getRequestURI() : requiresMFA.resourceId();
        
        boolean isValid = mfaTokenService.validateMFAToken(mfaToken, username, resourceType, resourceId);
        
        if (!isValid) {
            log.warn("Invalid MFA token for protected resource access by user: {}", username);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Invalid or expired MFA token\",\"mfaChallenge\":true}");
            return false;
        }

        log.info("MFA verification successful for user: {} accessing resource: {}/{}", username, resourceType, resourceId);
        return true;
    }
}
