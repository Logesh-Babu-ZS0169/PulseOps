package com.intics.metrics.scheduler;

import com.intics.metrics.config.AzureTokenProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@ConditionalOnProperty(name = "azure.token.enabled", havingValue = "true")
public class TokenRefreshScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshScheduler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AzureTokenProvider tokenProvider;

    @Autowired
    private DataSource dataSource;

    /**
     * Scheduled task to refresh Azure AD token every 65 minutes
     * Also re-establishes database connections with fresh token
     * Runs at fixed rate to ensure token is always fresh before expiry
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // 65 minutes in milliseconds
    public void refreshAzureToken() {
        String timestamp = LocalDateTime.now().format(formatter);
        
        logger.info("========================================");
        logger.info("Scheduled Azure AD Token Refresh Started");
        logger.info("Time: {}", timestamp);
        logger.info("========================================");

        try {
            // Step 1: Force refresh to get new token
            String token = tokenProvider.forceRefresh();
            
            // Get token info
            AzureTokenProvider.TokenInfo info = tokenProvider.getTokenInfo();
            
            logger.info("✓ Step 1: Azure AD token refreshed successfully");
            logger.info("  - Token expires at: {}", info.expiryTime);
            
            // Step 2: Update DataSource with new token
            updateDataSourcePassword(token);
            
            // Step 3: Test connection with new token
            testDatabaseConnection();
            
            logger.info("✓ Step 2: Database connection pool updated with new token");
            logger.info("✓ Step 3: Database connection verified successfully");
            logger.info("  - Next refresh in: 65 minutes");
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("========================================");
            logger.error("✗ Failed to refresh Azure AD token and database connection", e);
            logger.error("  - Error: {}", e.getMessage());
            logger.error("  - Will retry in 65 minutes");
            logger.error("========================================");
        }
    }

    /**
     * Update DataSource password with new Azure AD token
     */
    private void updateDataSourcePassword(String newToken) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            hikariDataSource.getHikariConfigMXBean().setPassword(newToken);
            
            // Evict idle connections to force using new token
            hikariDataSource.getHikariPoolMXBean().softEvictConnections();
            
            logger.debug("DataSource password updated with fresh Azure AD token");
            logger.debug("Idle connections evicted from pool");
        }
    }

    /**
     * Test database connection to verify new token works
     */
    private void testDatabaseConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                logger.debug("Database connection test successful with new token");
            } else {
                throw new SQLException("Database connection validation failed");
            }
        }
    }

    /**
     * Initial token generation on application startup
     * Runs 30 seconds after application starts
     */
    @Scheduled(initialDelay = 3000, fixedRate = Long.MAX_VALUE)
    public void initialTokenGeneration() {
        logger.info("========================================");
        logger.info("Initial Azure AD Token Generation");
        logger.info("========================================");
        
        try {
            // Generate initial token
            String token = tokenProvider.getAccessToken();
            AzureTokenProvider.TokenInfo info = tokenProvider.getTokenInfo();
            
            logger.info("✓ Initial Azure AD token generated successfully");
            logger.info("  - Token expires at: {}", info.expiryTime);
            
            // Update DataSource with initial token
            updateDataSourcePassword(token);
            
            // Test connection
            testDatabaseConnection();
            
            logger.info("✓ Database connection established with Azure AD token");
            logger.info("  - Scheduled refresh every: 65 minutes");
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("✗ Failed to generate initial Azure AD token and establish connection", e);
            logger.error("  - Error: {}", e.getMessage());
            logger.error("========================================");
        }
    }
}
