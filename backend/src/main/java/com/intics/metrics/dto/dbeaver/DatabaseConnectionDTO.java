package com.intics.metrics.dto.dbeaver;

public class DatabaseConnectionDTO {
    private String connectionName;
    private String provider;
    private String driver;
    private String host;
    private String port;
    private String database;
    private String username;
    private String jdbcUrl;
    private String sslMode;
    private boolean hasPassword;

    public DatabaseConnectionDTO() {}

    public DatabaseConnectionDTO(String connectionName, String provider, String driver, String host,
                                  String port, String database, String username, String jdbcUrl,
                                  String sslMode, boolean hasPassword) {
        this.connectionName = connectionName;
        this.provider = provider;
        this.driver = driver;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.jdbcUrl = jdbcUrl;
        this.sslMode = sslMode;
        this.hasPassword = hasPassword;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getSslMode() {
        return sslMode;
    }

    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }
}
