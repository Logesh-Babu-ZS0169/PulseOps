package com.intics.metrics.service;

import com.intics.metrics.dto.dbeaver.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DBeaverService {

    private final DataSource dataSource;

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    public DBeaverService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DatabaseConnectionDTO getConnectionInfo() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();

            String url = metadata.getURL();
            String user = metadata.getUserName();

            String host = extractHost(url);
            String port = extractPort(url);
            String database = extractDatabase(url);
            String sslMode = url.contains("sslmode=") ? "require" : "prefer";

            return new DatabaseConnectionDTO(
                "PulseOps Database",
                "postgresql",
                "postgres-jdbc",
                host,
                port,
                database,
                user,
                url,
                sslMode,
                true
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get connection info", e);
        }
    }

    public String generateDBeaverXML() {
        DatabaseConnectionDTO conn = getConnectionInfo();
        String connectionId = "pulseops-" + UUID.randomUUID().toString();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<data-sources>\n");
        xml.append("    <data-source id=\"").append(connectionId).append("\" provider=\"").append(conn.getProvider()).append("\" driver=\"").append(conn.getDriver()).append("\">\n");
        xml.append("        <connection\n");
        xml.append("            name=\"").append(conn.getConnectionName()).append("\"\n");
        xml.append("            host=\"").append(conn.getHost()).append("\"\n");
        xml.append("            port=\"").append(conn.getPort()).append("\"\n");
        xml.append("            server=\"\"\n");
        xml.append("            database=\"").append(conn.getDatabase()).append("\"\n");
        xml.append("            url=\"").append(conn.getJdbcUrl()).append("\"\n");
        xml.append("            user=\"").append(conn.getUsername()).append("\"\n");
        xml.append("            type=\"prod\"\n");
        xml.append("            save-password=\"false\"\n");
        xml.append("        />\n");
        xml.append("    </data-source>\n");
        xml.append("</data-sources>\n");

        return xml.toString();
    }

    public List<DatabaseSchemaDTO> getDatabaseSchemas() {
        List<DatabaseSchemaDTO> schemas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();

            ResultSet schemasRs = metadata.getSchemas();
            while (schemasRs.next()) {
                String schemaName = schemasRs.getString("TABLE_SCHEM");

                if (schemaName.equals("public") || schemaName.equals("information_schema") ||
                    schemaName.equals("config")) {
                    List<TableInfoDTO> tables = getTablesForSchema(conn, schemaName);
                    schemas.add(new DatabaseSchemaDTO(schemaName, tables));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database schemas", e);
        }

        return schemas;
    }

    private List<TableInfoDTO> getTablesForSchema(Connection conn, String schemaName) throws SQLException {
        List<TableInfoDTO> tables = new ArrayList<>();
        DatabaseMetaData metadata = conn.getMetaData();

        ResultSet tablesRs = metadata.getTables(null, schemaName, "%", new String[]{"TABLE", "VIEW"});

        while (tablesRs.next()) {
            String tableName = tablesRs.getString("TABLE_NAME");
            String tableType = tablesRs.getString("TABLE_TYPE");

            List<ColumnInfoDTO> columns = getColumnsForTable(metadata, schemaName, tableName);
            Long rowCount = getRowCount(conn, schemaName, tableName);

            tables.add(new TableInfoDTO(tableName, tableType, rowCount, columns));
        }

        return tables;
    }

    private List<ColumnInfoDTO> getColumnsForTable(DatabaseMetaData metadata, String schemaName, String tableName) throws SQLException {
        List<ColumnInfoDTO> columns = new ArrayList<>();

        ResultSet pkRs = metadata.getPrimaryKeys(null, schemaName, tableName);
        List<String> primaryKeys = new ArrayList<>();
        while (pkRs.next()) {
            primaryKeys.add(pkRs.getString("COLUMN_NAME"));
        }

        ResultSet columnsRs = metadata.getColumns(null, schemaName, tableName, "%");

        while (columnsRs.next()) {
            String columnName = columnsRs.getString("COLUMN_NAME");
            String dataType = columnsRs.getString("TYPE_NAME");
            boolean nullable = columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
            String defaultValue = columnsRs.getString("COLUMN_DEF");
            boolean isPk = primaryKeys.contains(columnName);

            columns.add(new ColumnInfoDTO(columnName, dataType, nullable, defaultValue, isPk));
        }

        return columns;
    }

    private Long getRowCount(Connection conn, String schemaName, String tableName) {
        try {
            String query = "SELECT COUNT(*) FROM \"" + schemaName + "\".\"" + tableName + "\"";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return 0L;
    }

    private String extractHost(String url) {
        try {
            String[] parts = url.split("//")[1].split("/")[0].split(":");
            return parts[0];
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String extractPort(String url) {
        try {
            String[] parts = url.split("//")[1].split("/")[0].split(":");
            if (parts.length > 1) {
                return parts[1];
            }
        } catch (Exception e) {
        }
        return "5432";
    }

    private String extractDatabase(String url) {
        try {
            String[] parts = url.split("/");
            String dbPart = parts[parts.length - 1];
            if (dbPart.contains("?")) {
                return dbPart.split("\\?")[0];
            }
            return dbPart;
        } catch (Exception e) {
            return "postgres";
        }
    }
}
