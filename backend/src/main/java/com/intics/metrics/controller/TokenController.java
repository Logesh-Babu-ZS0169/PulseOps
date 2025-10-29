package com.intics.metrics.controller;

import com.intics.metrics.config.AzureTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/token")
@ConditionalOnBean(AzureTokenProvider.class)
@Tag(name = "Azure Token", description = "Azure AD token management endpoints")
public class TokenController {

    @Autowired
    private AzureTokenProvider tokenProvider;

    @GetMapping
    @Operation(summary = "Get Azure AD access token", 
               description = "Returns the current cached Azure AD token or generates a new one if expired")
    public ResponseEntity<Map<String, Object>> getToken() {
        try {
            String token = tokenProvider.getAccessToken();
            AzureTokenProvider.TokenInfo info = tokenProvider.getTokenInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("hasToken", info.hasToken);
            response.put("expiryTime", info.expiryTime);
            response.put("isValid", info.isValid);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get token: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Force token refresh", 
               description = "Forces a new token to be generated, bypassing the cache")
    public ResponseEntity<Map<String, Object>> refreshToken() {
        try {
            String token = tokenProvider.forceRefresh();
            AzureTokenProvider.TokenInfo info = tokenProvider.getTokenInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Token refreshed successfully");
            response.put("expiryTime", info.expiryTime);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to refresh token: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/info")
    @Operation(summary = "Get token information", 
               description = "Returns information about the current token status without exposing the token value")
    public ResponseEntity<AzureTokenProvider.TokenInfo> getTokenInfo() {
        return ResponseEntity.ok(tokenProvider.getTokenInfo());
    }
}
