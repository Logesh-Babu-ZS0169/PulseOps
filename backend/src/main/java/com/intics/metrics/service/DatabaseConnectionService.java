package com.intics.metrics.service;

import com.intics.metrics.dto.database.*;
import com.intics.metrics.dto.database.TableMetadataDTO.ColumnMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DatabaseConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionService.class);

    private final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, DatabaseConnectionRequest> connectionConfigs = new ConcurrentHashMap<>();
    private final Map<String, String> connectionOwners = new ConcurrentHashMap<>();

    public DatabaseConnectionResponse testConnection(DatabaseConnectionRequest request) {
        try {
            Connection connection = createConnection(request);
            if (connection != null && !connection.isClosed()) {
                connection.close();
                return new DatabaseConnectionResponse(
                        null, request.getConnectionName(), request.getDatabaseType(),
                        request.getHost(), request.getPort(), request.getDatabaseName(),
                        request.getUsername(), true, "Connection successful"
                );
            }
        } catch (Exception e) {
            logger.error("Connection test failed", e);
            return new DatabaseConnectionResponse(
                    null, request.getConnectionName(), request.getDatabaseType(),
                    request.getHost(), request.getPort(), request.getDatabaseName(),
                    request.getUsername(), false, "Connection failed: " + e.getMessage()
            );
        }
        return new DatabaseConnectionResponse(
                null, request.getConnectionName(), request.getDatabaseType(),
                request.getHost(), request.getPort(), request.getDatabaseName(),
                request.getUsername(), false, "Connection failed"
        );
    }

    public DatabaseConnectionResponse createAndSaveConnection(DatabaseConnectionRequest request, String username) {
        try {
            Connection connection = createConnection(request);
            if (connection != null && !connection.isClosed()) {
                String connectionId = UUID.randomUUID().toString();
                activeConnections.put(connectionId, connection);
                connectionConfigs.put(connectionId, request);
                connectionOwners.put(connectionId, username);

                return new DatabaseConnectionResponse(
                        connectionId, request.getConnectionName(), request.getDatabaseType(),
                        request.getHost(), request.getPort(), request.getDatabaseName(),
                        request.getUsername(), true, "Connected successfully"
                );
            }
        } catch (Exception e) {
            logger.error("Failed to create connection", e);
            return new DatabaseConnectionResponse(
                    null, request.getConnectionName(), request.getDatabaseType(),
                    request.getHost(), request.getPort(), request.getDatabaseName(),
                    request.getUsername(), false, "Connection failed: " + e.getMessage()
            );
        }
        return new DatabaseConnectionResponse(
                null, request.getConnectionName(), request.getDatabaseType(),
                request.getHost(), request.getPort(), request.getDatabaseName(),
                request.getUsername(), false, "Connection failed"
        );
    }

    private Connection createConnection(DatabaseConnectionRequest request) throws SQLException {
        String jdbcUrl = buildJdbcUrl(request);
        Properties props = new Properties();
        props.setProperty("user", request.getUsername());
        props.setProperty("password", request.getPassword());

        if ("postgresql".equalsIgnoreCase(request.getDatabaseType())) {
            props.setProperty("ssl", "false");
        }

        return DriverManager.getConnection(jdbcUrl, props);
    }

    private String buildJdbcUrl(DatabaseConnectionRequest request) {
        String type = request.getDatabaseType().toLowerCase();
        String host = request.getHost();
        int port = request.getPort() != null ? request.getPort() : getDefaultPort(type);
        String database = request.getDatabaseName();

        switch (type) {
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s", host, port, database);
            case "mariadb":
                return String.format("jdbc:mariadb://%s:%d/%s", host, port, database);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }

    private int getDefaultPort(String databaseType) {
        switch (databaseType.toLowerCase()) {
            case "postgresql": return 5432;
            case "mysql": return 3306;
            case "mariadb": return 3306;
            case "oracle": return 1521;
            case "sqlserver": return 1433;
            default: return 5432;
        }
    }

    public List<SchemaDTO> getSchemas(String connectionId, String username) throws SQLException {
        validateConnectionOwnership(connectionId, username);
        Connection connection = getConnection(connectionId);
        DatabaseMetaData metaData = connection.getMetaData();
        List<SchemaDTO> schemas = new ArrayList<>();

        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                String schemaName = rs.getString("TABLE_SCHEM");
                if (!isSystemSchema(schemaName)) {
                    List<String> tables = getTablesInternal(connection, schemaName);
                    schemas.add(new SchemaDTO(schemaName, tables));
                }
            }
        }

        if (schemas.isEmpty()) {
            String defaultSchema = connectionConfigs.get(connectionId).getSchema();
            if (defaultSchema != null && !defaultSchema.isEmpty()) {
                List<String> tables = getTablesInternal(connection, defaultSchema);
                schemas.add(new SchemaDTO(defaultSchema, tables));
            } else {
                List<String> tables = getTablesInternal(connection, null);
                schemas.add(new SchemaDTO("public", tables));
            }
        }

        return schemas;
    }

    private boolean isSystemSchema(String schemaName) {
        String lower = schemaName.toLowerCase();
        return lower.equals("information_schema") ||
                lower.equals("pg_catalog") ||
                lower.equals("pg_toast") ||
                lower.startsWith("pg_temp") ||
                lower.startsWith("pg_toast_temp");
    }

    public List<String> getTables(String connectionId, String schemaName, String username) throws SQLException {
        validateConnectionOwnership(connectionId, username);
        Connection connection = getConnection(connectionId);
        return getTablesInternal(connection, schemaName);
    }

    private List<String> getTablesInternal(Connection connection, String schemaName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> tables = new ArrayList<>();

        try (ResultSet rs = metaData.getTables(null, schemaName, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        return tables;
    }

    public TableMetadataDTO getTableMetadata(String connectionId, String schemaName, String tableName, String username) throws SQLException {
        validateConnectionOwnership(connectionId, username);
        Connection connection = getConnection(connectionId);
        DatabaseMetaData metaData = connection.getMetaData();
        List<ColumnMetadata> columns = new ArrayList<>();

        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet pkRs = metaData.getPrimaryKeys(null, schemaName, tableName)) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                boolean isPk = primaryKeys.contains(columnName);

                columns.add(new ColumnMetadata(columnName, dataType, columnSize, nullable, isPk));
            }
        }

        long rowCount = getRowCount(connection, schemaName, tableName);

        return new TableMetadataDTO(tableName, schemaName, columns, rowCount);
    }

    private long getRowCount(Connection connection, String schemaName, String tableName) {
        String query = String.format("SELECT COUNT(*) FROM %s.%s",
                schemaName != null ? schemaName : "public", tableName);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warn("Failed to get row count for table: " + tableName, e);
        }
        return 0;
    }

    public TableDataDTO getTableData(String connectionId, String schemaName, String tableName,
                                     int page, int pageSize, String username) throws SQLException {
        validateConnectionOwnership(connectionId, username);
        Connection connection = getConnection(connectionId);

        String fullTableName = schemaName != null && !schemaName.isEmpty()
                ? schemaName + "." + tableName
                : tableName;

        long totalRows = getRowCount(connection, schemaName, tableName);
        int offset = (page - 1) * pageSize;

        String query = String.format("SELECT * FROM %s LIMIT %d OFFSET %d",
                fullTableName, pageSize, offset);

        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnCount = rsMetaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                columns.add(rsMetaData.getColumnName(i));
            }

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.put(columns.get(i - 1), value);
                }
                rows.add(row);
            }
        }

        return new TableDataDTO(tableName, schemaName, columns, rows,
                (int) totalRows, pageSize, page);
    }

    public List<DatabaseConnectionResponse> getAllConnections(String username) {
        List<DatabaseConnectionResponse> connections = new ArrayList<>();
        for (Map.Entry<String, DatabaseConnectionRequest> entry : connectionConfigs.entrySet()) {
            String connectionId = entry.getKey();
            String owner = connectionOwners.get(connectionId);

            if (username.equals(owner)) {
                DatabaseConnectionRequest config = entry.getValue();
                boolean isConnected = activeConnections.containsKey(connectionId);

                connections.add(new DatabaseConnectionResponse(
                        connectionId, config.getConnectionName(), config.getDatabaseType(),
                        config.getHost(), config.getPort(), config.getDatabaseName(),
                        config.getUsername(), isConnected,
                        isConnected ? "Connected" : "Disconnected"
                ));
            }
        }
        return connections;
    }

    public void closeConnection(String connectionId, String username) throws SQLException {
        validateConnectionOwnership(connectionId, username);
        Connection connection = activeConnections.remove(connectionId);
        connectionConfigs.remove(connectionId);
        connectionOwners.remove(connectionId);
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public Connection getConnectionForAnalytics(String connectionId, String username) throws SQLException {
        validateConnectionOwnership(connectionId, username);
        return getConnection(connectionId);
    }

    public DatabaseConnectionResponse getConnectionInfo(String connectionId, String username) {
        validateConnectionOwnership(connectionId, username);
        DatabaseConnectionRequest config = connectionConfigs.get(connectionId);
        if (config == null) {
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }
        boolean isConnected = activeConnections.containsKey(connectionId);
        return new DatabaseConnectionResponse(
                connectionId, config.getConnectionName(), config.getDatabaseType(),
                config.getHost(), config.getPort(), config.getDatabaseName(),
                config.getUsername(), isConnected,
                isConnected ? "Connected" : "Disconnected"
        );
    }

    private void validateConnectionOwnership(String connectionId, String username) {
        String owner = connectionOwners.get(connectionId);
        if (owner == null) {
            logger.error("Connection ownership validation failed: Connection ID {} not found", connectionId);
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }
        if (!owner.equals(username)) {
            logger.error("Connection ownership validation failed: owner='{}', requestingUser='{}'", owner, username);
            throw new SecurityException("Access denied: You do not own this connection");
        }
        logger.debug("Connection ownership validated successfully for user '{}' accessing connection '{}'", username, connectionId);
    }

    private Connection getConnection(String connectionId) throws SQLException {
        Connection connection = activeConnections.get(connectionId);
        if (connection == null) {
            throw new SQLException("Connection not found: " + connectionId);
        }
        if (connection.isClosed()) {
            DatabaseConnectionRequest config = connectionConfigs.get(connectionId);
            if (config != null) {
                connection = createConnection(config);
                activeConnections.put(connectionId, connection);
            } else {
                throw new SQLException("Connection is closed and config not found: " + connectionId);
            }
        }
        return connection;
    }
}
