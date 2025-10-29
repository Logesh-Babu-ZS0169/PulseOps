package com.intics.metrics.service;

import com.intics.metrics.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MFATokenService {

    @Value("${mfa.token.secret:mfaSessionSecretKey2024ThisShouldBeChangedInProduction12345678901234567890}")
    private String secret;

    private static final long MFA_SESSION_EXPIRATION = 300000; // 5 minutes

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateMFASessionToken(String username, String resourceType, String resourceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("resourceType", resourceType);
        claims.put("resourceId", resourceId);
        claims.put("mfaVerified", true);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + MFA_SESSION_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateMFAToken(String token, String username, String resourceType, String resourceId) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenUsername = claims.getSubject();
            String tokenResourceType = claims.get("resourceType", String.class);
            String tokenResourceId = claims.get("resourceId", String.class);
            Boolean mfaVerified = claims.get("mfaVerified", Boolean.class);
            Date expiration = claims.getExpiration();

            return tokenUsername.equals(username) &&
                   tokenResourceType.equals(resourceType) &&
                   tokenResourceId.equals(resourceId) &&
                   Boolean.TRUE.equals(mfaVerified) &&
                   expiration.after(new Date());
        } catch (Exception e) {
            log.error("MFA token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from MFA token: {}", e.getMessage());
            return null;
        }
    }
}
