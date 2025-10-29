package com.intics.metrics.config;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ConditionalOnProperty(name = "azure.token.enabled", havingValue = "true")
public class AzureTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(AzureTokenProvider.class);

    @Value("${azure.identity.tenantId}")
    private String tenantId;

    @Value("${azure.identity.clientId}")
    private String clientId;

    @Value("${azure.identity.clientSecret}")
    private String clientSecret;

    @Value("${azure.token.scope:https://ossrdbms-aad.database.windows.net/.default}")
    private String azureDatabaseTokenScope;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();

    private static final Duration REFRESH_SAFETY_MARGIN = Duration.ofMinutes(5);

    /**
     * Get a valid access token, refreshing if necessary
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            return cachedToken;
        }

        tokenRefreshLock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return cachedToken;
            }

            refreshToken();
            return cachedToken;

        } finally {
            tokenRefreshLock.unlock();
        }
    }

    /**
     * Check if the cached token is still valid
     */
    private boolean isTokenValid() {
        if (cachedToken == null || tokenExpiry == null) {
            logger.debug("No cached Azure AD token available");
            return false;
        }

        Instant refreshThreshold = tokenExpiry.minus(REFRESH_SAFETY_MARGIN);
        boolean isValid = Instant.now().isBefore(refreshThreshold);

        if (!isValid) {
            logger.debug("Cached Azure AD token expires at {}, refresh threshold passed", tokenExpiry);
        }
        return isValid;
    }

    /**
     * Refresh the Azure AD token with retry logic
     */
    private void refreshToken() {
        logger.info("Refreshing Azure AD token for database authentication...");

        int maxRetries = 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                        .tenantId(tenantId)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build();

                TokenRequestContext requestContext = new TokenRequestContext()
                        .addScopes(azureDatabaseTokenScope);
                
                AccessToken token = credential.getToken(requestContext).block();

                if (token == null) {
                    throw new IllegalStateException("Azure AD token request returned null");
                }

                this.cachedToken = token.getToken();
                this.tokenExpiry = token.getExpiresAt().toInstant();

                Instant nextRefresh = tokenExpiry.minus(REFRESH_SAFETY_MARGIN);
                Duration timeUntilRefresh = Duration.between(Instant.now(), nextRefresh);

                logger.info("Azure AD token refreshed successfully. Expires: {}, Next refresh in: {} minutes", 
                    tokenExpiry, timeUntilRefresh.toMinutes());
                return;

            } catch (Exception e) {
                lastException = e;
                logger.warn("Azure AD token refresh attempt {} of {} failed: {}", 
                    attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    long delayMs = 1000 * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted during Azure AD token refresh retry", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Failed to refresh Azure AD token after " + maxRetries + " attempts", lastException);
    }

    /**
     * Force refresh the token, bypassing cache
     */
    public String forceRefresh() {
        tokenRefreshLock.lock();
        try {
            logger.info("Forcing Azure AD token refresh (bypassing cache)");
            refreshToken();
            return cachedToken;
        } finally {
            tokenRefreshLock.unlock();
        }
    }

    /**
     * Get information about the current token
     */
    public TokenInfo getTokenInfo() {
        return new TokenInfo(
                cachedToken != null,
                tokenExpiry,
                isTokenValid()
        );
    }

    public static class TokenInfo {
        public final boolean hasToken;
        public final Instant expiryTime;
        public final boolean isValid;

        public TokenInfo(boolean hasToken, Instant expiryTime, boolean isValid) {
            this.hasToken = hasToken;
            this.expiryTime = expiryTime;
            this.isValid = isValid;
        }

        @Override
        public String toString() {
            return String.format("TokenInfo{hasToken=%s, expiryTime=%s, isValid=%s}",
                    hasToken, expiryTime, isValid);
        }
    }
}
