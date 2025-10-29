package com.intics.metrics.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
@ConditionalOnProperty(name = "azure.token.enabled", havingValue = "true")
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Autowired
    private AzureTokenProvider tokenProvider;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Primary
    @Bean
    public DataSource dataSource() {
        logger.info("=== Configuring DataSource with Azure AD Token Authentication ===");
        
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Get initial Azure AD token
        String initialToken = tokenProvider.getAccessToken();
        config.setPassword(initialToken);
        
        // Connection pool settings
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // Pool name
        config.setPoolName("AzureToken-HikariPool");
        
        // Auto-commit
        config.setAutoCommit(true);
        
        // Connection test query
        config.setConnectionTestQuery("SELECT 1");
        
        // Create custom DataSource that refreshes token
        TokenRefreshingDataSource dataSource = new TokenRefreshingDataSource(config, tokenProvider);
        
        logger.info("DataSource configured with Azure AD token authentication");
        logger.info("Database URL: {}", jdbcUrl);
        logger.info("Database User: {}", username);
        
        return dataSource;
    }

    /**
     * Custom HikariDataSource that automatically refreshes Azure AD token
     */
    private static class TokenRefreshingDataSource extends HikariDataSource {
        
        private final AzureTokenProvider tokenProvider;
        private final Logger logger = LoggerFactory.getLogger(TokenRefreshingDataSource.class);

        public TokenRefreshingDataSource(HikariConfig configuration, AzureTokenProvider tokenProvider) {
            super(configuration);
            this.tokenProvider = tokenProvider;
        }

        @Override
        public Connection getConnection() throws SQLException {
            try {
                // Get fresh Azure AD token
                String token = tokenProvider.getAccessToken();
                
                // Update DataSource password with fresh token
                this.getHikariConfigMXBean().setPassword(token);
                
                logger.debug("Using fresh Azure AD token for database connection");
                
                // Get connection with updated token
                return super.getConnection();
                
            } catch (Exception e) {
                logger.error("Failed to get database connection with Azure AD token: {}", e.getMessage());
                throw new SQLException("Database connection failed with Azure AD token authentication", e);
            }
        }
    }
}
