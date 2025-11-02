package com.intics.metrics.service;

import com.intics.metrics.dto.analytics.*;
import com.intics.metrics.dto.database.DatabaseConnectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class DatabaseAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseAnalyticsService.class);

    @Autowired
    private DatabaseConnectionService connectionService;

    private static class TableMetadata {
        String schema;
        String name;
        long rowCount;
        double sizeMB;
        int columnCount;

        TableMetadata(String schema, String name, long rowCount, double sizeMB, int columnCount) {
            this.schema = schema;
            this.name = name;
            this.rowCount = rowCount;
            this.sizeMB = sizeMB;
            this.columnCount = columnCount;
        }
    }

    private Map<String, TableMetadata> buildTableMetadataCache(Connection connection, String databaseType) throws SQLException {
        Map<String, TableMetadata> cache = new ConcurrentHashMap<>();

        if (databaseType.equalsIgnoreCase("POSTGRESQL")) {
            return buildPostgresMetadataCache(connection);
        }

        DatabaseMetaData metaData = connection.getMetaData();

        ResultSet schemas = metaData.getSchemas();
        List<String> userSchemas = new ArrayList<>();
        while (schemas.next()) {
            String schemaName = schemas.getString("TABLE_SCHEM");
            if (isUserSchema(schemaName, databaseType)) {
                userSchemas.add(schemaName);
            }
        }
        schemas.close();

        logger.info("Building metadata cache for ALL tables in {} schemas...", userSchemas.size());
        long startTime = System.currentTimeMillis();

        int tableCount = 0;
        for (String schemaName : userSchemas) {
            ResultSet tables = metaData.getTables(null, schemaName, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String key = schemaName + "." + tableName;

                long rowCount = getTableRowCount(connection, schemaName, tableName, databaseType);
                double sizeMB = getTableSize(connection, schemaName, tableName, databaseType);
                int columnCount = getColumnCount(metaData, schemaName, tableName);

                cache.put(key, new TableMetadata(schemaName, tableName, rowCount, sizeMB, columnCount));
                tableCount++;

                if (tableCount % 500 == 0) {
                    logger.info("Processed {} tables...", tableCount);
                }
            }
            tables.close();
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Built metadata cache for {} tables in {} ms", cache.size(), duration);
        return cache;
    }

    private Map<String, TableMetadata> buildPostgresMetadataCache(Connection connection) throws SQLException {
        Map<String, TableMetadata> cache = new ConcurrentHashMap<>();

        logger.info("Building complete Postgres metadata cache for ALL tables...");
        long startTime = System.currentTimeMillis();

        String query =
                "SELECT " +
                        "  s.schemaname, " +
                        "  s.relname AS tablename, " +
                        "  s.n_live_tup AS row_count, " +
                        "  pg_total_relation_size(quote_ident(s.schemaname) || '.' || quote_ident(s.relname)) / 1024.0 / 1024.0 AS size_mb, " +
                        "  (SELECT count(*) FROM information_schema.columns c " +
                        "   WHERE c.table_schema = s.schemaname AND c.table_name = s.relname) AS column_count " +
                        "FROM pg_stat_user_tables s " +
                        "WHERE s.schemaname NOT IN ('pg_catalog', 'information_schema') " +
                        "ORDER BY s.n_live_tup DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            int count = 0;
            while (rs.next()) {
                String schema = rs.getString("schemaname");
                String tableName = rs.getString("tablename");
                long rowCount = rs.getLong("row_count");
                double sizeMB = rs.getDouble("size_mb");
                int columnCount = rs.getInt("column_count");

                String key = schema + "." + tableName;
                cache.put(key, new TableMetadata(schema, tableName, rowCount, sizeMB, columnCount));
                count++;

                if (count % 1000 == 0) {
                    logger.info("Processed {} tables...", count);
                }
            }
        } catch (Exception e) {
            logger.error("Error building Postgres metadata cache: {}", e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Built Postgres metadata cache for {} tables in {} ms", cache.size(), duration);
        return cache;
    }

    private int getColumnCountQuick(Connection conn, String schema, String table) {
        try {
            String query = "SELECT count(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, schema);
                stmt.setString(2, table);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get column count: {}", e.getMessage());
        }
        return 0;
    }

    private int getTotalSchemaCount(Connection conn, String databaseType) {
        try {
            String query;
            if (databaseType.equalsIgnoreCase("POSTGRESQL")) {
                query = "SELECT COUNT(DISTINCT schemaname) FROM pg_stat_user_tables";
            } else if (databaseType.equalsIgnoreCase("MYSQL") || databaseType.equalsIgnoreCase("MARIADB")) {
                query = "SELECT COUNT(DISTINCT table_schema) FROM information_schema.tables WHERE table_schema NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')";
            } else if (databaseType.equalsIgnoreCase("ORACLE")) {
                query = "SELECT COUNT(DISTINCT owner) FROM all_tables WHERE owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'CTXSYS', 'XDB')";
            } else if (databaseType.equalsIgnoreCase("SQL SERVER")) {
                query = "SELECT COUNT(DISTINCT schema_name) FROM information_schema.tables WHERE table_schema NOT IN ('sys', 'INFORMATION_SCHEMA')";
            } else {
                return 0;
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get total schema count: {}", e.getMessage());
        }
        return 0;
    }

    private int getTotalTableCount(Connection conn, String databaseType) {
        try {
            String query;
            if (databaseType.equalsIgnoreCase("POSTGRESQL")) {
                query = "SELECT COUNT(*) FROM pg_stat_user_tables";
            } else if (databaseType.equalsIgnoreCase("MYSQL") || databaseType.equalsIgnoreCase("MARIADB")) {
                query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys') AND table_type = 'BASE TABLE'";
            } else if (databaseType.equalsIgnoreCase("ORACLE")) {
                query = "SELECT COUNT(*) FROM all_tables WHERE owner NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'OUTLN', 'MDSYS', 'ORDSYS', 'CTXSYS', 'XDB')";
            } else if (databaseType.equalsIgnoreCase("SQL SERVER")) {
                query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema NOT IN ('sys', 'INFORMATION_SCHEMA') AND table_type = 'BASE TABLE'";
            } else {
                return 0;
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get total table count: {}", e.getMessage());
        }
        return 0;
    }

    public DatabaseOverviewDTO getDatabaseOverview(String connectionId, String username) throws Exception {
        DatabaseConnectionResponse connInfo = connectionService.getConnectionInfo(connectionId, username);
        Connection connection = connectionService.getConnectionForAnalytics(connectionId, username);
        try {
            DatabaseOverviewDTO overview = new DatabaseOverviewDTO();
            overview.setDatabaseName(connInfo.getDatabaseName());

            String databaseType = connInfo.getDatabaseType();

            int actualTotalSchemas = getTotalSchemaCount(connection, databaseType);
            int actualTotalTables = getTotalTableCount(connection, databaseType);

            Map<String, TableMetadata> metadataCache = buildTableMetadataCache(connection, databaseType);

            Map<String, SchemaInfo> schemaMap = new HashMap<>();
            List<DatabaseOverviewDTO.TableSizeInfo> allTables = new ArrayList<>();

            long totalRows = 0;
            double totalSizeMB = 0.0;

            for (TableMetadata tm : metadataCache.values()) {
                schemaMap.putIfAbsent(tm.schema, new SchemaInfo(tm.schema));
                SchemaInfo si = schemaMap.get(tm.schema);

                si.tableCount++;
                si.sizeMB += tm.sizeMB;
                totalRows += tm.rowCount;
                totalSizeMB += tm.sizeMB;

                allTables.add(new DatabaseOverviewDTO.TableSizeInfo(
                        tm.schema, tm.name, tm.rowCount, tm.sizeMB
                ));
            }

            overview.setTotalSchemas(actualTotalSchemas);
            overview.setTotalTables(actualTotalTables);
            overview.setTotalRows(totalRows);
            overview.setTotalSizeMB(totalSizeMB);

            List<DatabaseOverviewDTO.SchemaStats> schemaStats = schemaMap.values().stream()
                    .map(si -> new DatabaseOverviewDTO.SchemaStats(si.name, si.tableCount, si.sizeMB))
                    .collect(Collectors.toList());
            overview.setSchemaStats(schemaStats);

            List<DatabaseOverviewDTO.TableSizeInfo> largest = allTables.stream()
                    .sorted((a, b) -> Double.compare(b.getSizeMB(), a.getSizeMB()))
                    .limit(10)
                    .collect(Collectors.toList());
            overview.setLargestTables(largest);

            return overview;
        } catch (SQLException e) {
            logger.error("Error getting database overview: {}", e.getMessage());
            throw new Exception("Failed to analyze database overview", e);
        }
    }

    public TableHealthDTO getTableHealth(String connectionId, String username) throws Exception {
        DatabaseConnectionResponse connInfo = connectionService.getConnectionInfo(connectionId, username);
        Connection connection = connectionService.getConnectionForAnalytics(connectionId, username);
        try {
            TableHealthDTO health = new TableHealthDTO();
            String databaseType = connInfo.getDatabaseType();
            Map<String, TableMetadata> metadataCache = buildTableMetadataCache(connection, databaseType);

            List<TableHealthDTO.EmptyTable> emptyTables = new ArrayList<>();
            List<TableHealthDTO.UnusedTable> unusedTables = new ArrayList<>();
            List<TableHealthDTO.TableStats> tableStats = new ArrayList<>();

            for (TableMetadata tm : metadataCache.values()) {
                if (tm.rowCount == 0) {
                    emptyTables.add(new TableHealthDTO.EmptyTable(tm.schema, tm.name, tm.sizeMB));
                } else if (tm.rowCount > 0 && tm.rowCount < 100 && tm.sizeMB < 1.0) {
                    unusedTables.add(new TableHealthDTO.UnusedTable(
                            tm.schema, tm.name, tm.rowCount, tm.sizeMB,
                            "Table has very few rows (" + tm.rowCount + ") and minimal size, may be unused or test data"
                    ));
                }

                String healthStatus = determineTableHealth(tm.rowCount, tm.columnCount, tm.sizeMB);
                tableStats.add(new TableHealthDTO.TableStats(
                        tm.schema, tm.name, tm.rowCount, tm.columnCount, tm.sizeMB, healthStatus
                ));
            }

            health.setEmptyTables(emptyTables);
            health.setUnusedTables(unusedTables);
            health.setTableStatistics(tableStats);

            return health;
        } catch (SQLException e) {
            logger.error("Error analyzing table health: {}", e.getMessage());
            throw new Exception("Failed to analyze table health", e);
        }
    }

    public ColumnHealthDTO getColumnHealth(String connectionId, String username) throws Exception {
        DatabaseConnectionResponse connInfo = connectionService.getConnectionInfo(connectionId, username);
        Connection connection = connectionService.getConnectionForAnalytics(connectionId, username);
        try {
            ColumnHealthDTO health = new ColumnHealthDTO();
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseType = connInfo.getDatabaseType();
            Map<String, TableMetadata> metadataCache = buildTableMetadataCache(connection, databaseType);

            List<ColumnHealthDTO.ProblematicColumn> nullHeavy = new ArrayList<>();
            int totalColumns = 0;
            int problematic = 0;
            int tablesAnalyzed = 0;

            for (TableMetadata tm : metadataCache.values()) {
                if (tm.rowCount == 0 || tm.rowCount > 100000) continue;
                if (tablesAnalyzed >= 100) break;

                tablesAnalyzed++;

                ResultSet columns = metaData.getColumns(null, tm.schema, tm.name, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dataType = columns.getString("TYPE_NAME");
                    totalColumns++;

                    double nullPct = calculateNullPercentage(connection, tm.schema, tm.name, columnName, tm.rowCount);

                    if (nullPct >= 95.0) {
                        problematic++;
                        nullHeavy.add(new ColumnHealthDTO.ProblematicColumn(
                                tm.schema, tm.name, columnName, dataType, nullPct,
                                "High NULL percentage (" + String.format("%.1f", nullPct) + "%)",
                                "Consider dropping this column or making it non-nullable with a default value"
                        ));
                    }
                }
                columns.close();
            }

            health.setNullHeavyColumns(nullHeavy.stream().limit(20).collect(Collectors.toList()));
            health.setRedundantColumns(new ArrayList<>());
            health.setTotalColumnsAnalyzed(totalColumns);
            health.setHealthyColumns(totalColumns - problematic);
            health.setProblematicColumns(problematic);

            return health;
        } catch (SQLException e) {
            logger.error("Error analyzing column health: {}", e.getMessage());
            throw new Exception("Failed to analyze column health", e);
        }
    }

    public SchemaRelationshipsDTO getSchemaRelationships(String connectionId, String username) throws Exception {
        DatabaseConnectionResponse connInfo = connectionService.getConnectionInfo(connectionId, username);
        Connection connection = connectionService.getConnectionForAnalytics(connectionId, username);
        try {
            SchemaRelationshipsDTO relationships = new SchemaRelationshipsDTO();
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseType = connInfo.getDatabaseType();
            Map<String, TableMetadata> metadataCache = buildTableMetadataCache(connection, databaseType);

            List<SchemaRelationshipsDTO.Relationship> rels = new ArrayList<>();
            Set<String> connectedTables = new HashSet<>();

            for (TableMetadata tm : metadataCache.values()) {
                ResultSet fks = metaData.getImportedKeys(null, tm.schema, tm.name);
                while (fks.next()) {
                    String fkSchema = fks.getString("FKTABLE_SCHEM");
                    String fkTable = fks.getString("FKTABLE_NAME");
                    String fkColumn = fks.getString("FKCOLUMN_NAME");
                    String pkSchema = fks.getString("PKTABLE_SCHEM");
                    String pkTable = fks.getString("PKTABLE_NAME");
                    String pkColumn = fks.getString("PKCOLUMN_NAME");
                    String fkName = fks.getString("FK_NAME");

                    rels.add(new SchemaRelationshipsDTO.Relationship(
                            fkSchema, fkTable, fkColumn, pkSchema, pkTable, pkColumn, fkName
                    ));

                    connectedTables.add(fkSchema + "." + fkTable);
                    connectedTables.add(pkSchema + "." + pkTable);
                }
                fks.close();
            }

            List<SchemaRelationshipsDTO.IsolatedTable> isolated = metadataCache.entrySet().stream()
                    .filter(e -> !connectedTables.contains(e.getKey()))
                    .map(e -> {
                        TableMetadata tm = e.getValue();
                        return new SchemaRelationshipsDTO.IsolatedTable(
                                tm.schema, tm.name, tm.rowCount, "No foreign key relationships"
                        );
                    })
                    .collect(Collectors.toList());

            relationships.setRelationships(rels);
            relationships.setIsolatedTables(isolated);
            relationships.setTotalRelationships(rels.size());
            relationships.setConnectedTables(connectedTables.size());
            relationships.setIsolatedTableCount(isolated.size());

            return relationships;
        } catch (SQLException e) {
            logger.error("Error analyzing schema relationships: {}", e.getMessage());
            throw new Exception("Failed to analyze schema relationships", e);
        }
    }

    public OptimizationSuggestionsDTO getOptimizationSuggestions(String connectionId, String username) throws Exception {
        DatabaseConnectionResponse connInfo = connectionService.getConnectionInfo(connectionId, username);
        Connection connection = connectionService.getConnectionForAnalytics(connectionId, username);

        try {
            String databaseType = connInfo.getDatabaseType();
            Map<String, TableMetadata> metadataCache = buildTableMetadataCache(connection, databaseType);

            OptimizationSuggestionsDTO suggestions = new OptimizationSuggestionsDTO();
            List<OptimizationSuggestionsDTO.Suggestion> suggestionList = new ArrayList<>();

            int high = 0, medium = 0, low = 0;

            for (TableMetadata tm : metadataCache.values()) {
                if (tm.rowCount == 0 && tm.sizeMB > 1.0) {
                    String priority = tm.sizeMB > 10 ? "HIGH" : "MEDIUM";
                    if (priority.equals("HIGH")) high++; else medium++;

                    suggestionList.add(new OptimizationSuggestionsDTO.Suggestion(
                            "Storage", priority,
                            "Drop Empty Table: " + tm.name,
                            "Table has zero rows but consumes " + String.format("%.2f", tm.sizeMB) + " MB",
                            tm.schema + "." + tm.name,
                            tm.sizeMB,
                            Arrays.asList(
                                    "Verify table is truly unused",
                                    "Backup table definition if needed",
                                    "Execute: DROP TABLE " + tm.schema + "." + tm.name
                            )
                    ));
                }

                if (tm.rowCount > 0 && tm.rowCount < 100 && tm.sizeMB < 1.0) {
                    low++;
                    suggestionList.add(new OptimizationSuggestionsDTO.Suggestion(
                            "Cleanup", "LOW",
                            "Review Low-Usage Table: " + tm.name,
                            "Table has only " + tm.rowCount + " rows and may be test data",
                            tm.schema + "." + tm.name,
                            0.0,
                            Arrays.asList(
                                    "Verify if this is production or test data",
                                    "Consider archiving or removing if obsolete"
                            )
                    ));
                }

                if (tm.sizeMB > 1000) {
                    medium++;
                    suggestionList.add(new OptimizationSuggestionsDTO.Suggestion(
                            "Performance", "MEDIUM",
                            "Review Large Table: " + tm.name,
                            "Table size is " + String.format("%.2f", tm.sizeMB) + " MB with " + tm.rowCount + " rows",
                            tm.schema + "." + tm.name,
                            0.0,
                            Arrays.asList(
                                    "Consider partitioning if query patterns allow",
                                    "Review indexing strategy",
                                    "Consider archiving old data"
                            )
                    ));
                }
            }

            suggestions.setSuggestions(suggestionList.stream()
                    .sorted((a, b) -> {
                        int priorityOrder = getPriorityValue(b.getPriority()) - getPriorityValue(a.getPriority());
                        if (priorityOrder != 0) return priorityOrder;
                        return Double.compare(b.getPotentialSavingsMB(), a.getPotentialSavingsMB());
                    })
                    .limit(20)
                    .collect(Collectors.toList()));
            suggestions.setHighPriority(high);
            suggestions.setMediumPriority(medium);
            suggestions.setLowPriority(low);
            suggestions.setTotalSuggestions(suggestionList.size());

            return suggestions;
        } catch (Exception e) {
            logger.error("Error generating optimization suggestions: {}", e.getMessage());
            throw new Exception("Failed to generate optimization suggestions", e);
        }
    }

    private int getPriorityValue(String priority) {
        switch (priority) {
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW": return 1;
            default: return 0;
        }
    }

    private boolean isUserSchema(String schemaName, String dbType) {
        if (schemaName == null) return false;
        String lower = schemaName.toLowerCase();

        if (dbType.equalsIgnoreCase("POSTGRESQL")) {
            return !lower.equals("pg_catalog") && !lower.equals("information_schema") &&
                    !lower.startsWith("pg_toast") && !lower.startsWith("pg_temp");
        } else if (dbType.equalsIgnoreCase("MYSQL") || dbType.equalsIgnoreCase("MARIADB")) {
            return !lower.equals("information_schema") && !lower.equals("mysql") &&
                    !lower.equals("performance_schema") && !lower.equals("sys");
        } else if (dbType.equalsIgnoreCase("ORACLE")) {
            return !lower.startsWith("sys") && !lower.equals("system") &&
                    !lower.startsWith("apex_") && !lower.startsWith("flows_");
        } else if (dbType.equalsIgnoreCase("SQL_SERVER")) {
            return !lower.equals("sys") && !lower.equals("information_schema") &&
                    !lower.startsWith("db_");
        }

        return true;
    }

    private long getTableRowCount(Connection conn, String schema, String table, String dbType) {
        try {
            if (dbType.equalsIgnoreCase("POSTGRESQL")) {
                String query = "SELECT n_live_tup FROM pg_stat_user_tables WHERE schemaname = ? AND relname = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, schema);
                    stmt.setString(2, table);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                    }
                }
            } else if (dbType.contains("mysql") || dbType.contains("mariadb")) {
                String query = "SELECT table_rows FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, schema);
                    stmt.setString(2, table);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                    }
                }
            } else if (dbType.contains("oracle")) {
                String query = "SELECT num_rows FROM all_tables WHERE owner = ? AND table_name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, schema.toUpperCase());
                    stmt.setString(2, table.toUpperCase());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            long rows = rs.getLong(1);
                            return rows > 0 ? rows : 0;
                        }
                    }
                }
            } else if (dbType.contains("sql server") || dbType.contains("microsoft")) {
                String query = "SELECT SUM(p.rows) FROM sys.partitions p " +
                        "INNER JOIN sys.tables t ON p.object_id = t.object_id " +
                        "INNER JOIN sys.schemas s ON t.schema_id = s.schema_id " +
                        "WHERE s.name = ? AND t.name = ? AND p.index_id IN (0,1)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, schema);
                    stmt.setString(2, table);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not get row count for {}.{}: {}", schema, table, e.getMessage());
        }
        return 0;
    }

    private double getTableSize(Connection conn, String schema, String table, String dbType) {
        if (dbType.equalsIgnoreCase("POSTGRESQL")) {
            String query = "SELECT pg_total_relation_size('" + schema + "." + table + "') / 1024.0 / 1024.0";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            } catch (Exception e) {
                logger.debug("Could not get size for {}.{}: {}", schema, table, e.getMessage());
            }
        }
        return 0.0;
    }

    private int getColumnCount(DatabaseMetaData metaData, String schema, String table) throws SQLException {
        int count = 0;
        ResultSet columns = metaData.getColumns(null, schema, table, "%");
        while (columns.next()) {
            count++;
        }
        columns.close();
        return count;
    }

    private double calculateNullPercentage(Connection conn, String schema, String table, String column, long totalRows) {
        if (totalRows == 0 || totalRows > 5000) return 0.0;

        try {
            String dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();
            String query;

            if (dbType.contains("sql server") || dbType.contains("microsoft")) {
                query = "SELECT TOP 1000 COUNT(*) FROM " + quoteIdentifier(schema) + "." + quoteIdentifier(table) +
                        " WHERE " + quoteIdentifier(column) + " IS NULL";
            } else if (dbType.contains("oracle")) {
                query = "SELECT COUNT(*) FROM (SELECT * FROM " + quoteIdentifier(schema) + "." + quoteIdentifier(table) +
                        " WHERE " + quoteIdentifier(column) + " IS NULL AND ROWNUM <= 1000)";
            } else {
                query = "SELECT COUNT(*) FROM " + quoteIdentifier(schema) + "." + quoteIdentifier(table) +
                        " WHERE " + quoteIdentifier(column) + " IS NULL LIMIT 1000";
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    long nullCount = rs.getLong(1);
                    return (nullCount * 100.0) / Math.min(totalRows, 1000);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not calculate null % for {}.{}.{}: {}", schema, table, column, e.getMessage());
        }
        return 0.0;
    }

    private String determineTableHealth(long rowCount, int columnCount, double sizeMB) {
        if (rowCount == 0) return "EMPTY";
        if (rowCount < 10) return "VERY_LOW";
        if (sizeMB > 1000) return "LARGE";
        if (rowCount > 1000000) return "HIGH_VOLUME";
        return "HEALTHY";
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static class SchemaInfo {
        String name;
        int tableCount = 0;
        double sizeMB = 0.0;

        SchemaInfo(String name) {
            this.name = name;
        }
    }

    public IndexRecommendationDTO analyzeIndexes(String connectionId, String username) throws Exception {
        Connection conn = connectionService.getConnectionForAnalytics(connectionId, username);
        String dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (!dbType.contains("postgres")) {
            throw new IllegalArgumentException("Index analysis currently only supports PostgreSQL");
        }

        IndexRecommendationDTO result = new IndexRecommendationDTO();
        List<IndexRecommendationDTO.MissingIndex> missingIndexes = new ArrayList<>();
        List<IndexRecommendationDTO.UnusedIndex> unusedIndexes = new ArrayList<>();

        String missingIndexQuery = "SELECT schemaname, relname AS tablename, seq_scan, idx_scan, " +
                "COALESCE(seq_scan, 0) as sequential_scans, COALESCE(idx_scan, 0) as index_scans " +
                "FROM pg_stat_user_tables WHERE seq_scan > 100 ORDER BY seq_scan DESC LIMIT 20";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(missingIndexQuery)) {
            while (rs.next()) {
                String schema = rs.getString("schemaname");
                String table = rs.getString("tablename");
                long seqScans = rs.getLong("sequential_scans");
                long idxScans = rs.getLong("index_scans");

                String priority = seqScans > 1000 ? "HIGH" : seqScans > 500 ? "MEDIUM" : "LOW";
                String suggestion = String.format("Table has %d sequential scans. Consider adding indexes on frequently filtered columns.", seqScans);
                String proposedIndex = String.format("CREATE INDEX CONCURRENTLY idx_%s_<column> ON %s.<column>;", table, table);

                missingIndexes.add(new IndexRecommendationDTO.MissingIndex(
                        schema, table, seqScans, idxScans, suggestion, proposedIndex, priority
                ));
            }
        }

        String unusedIndexQuery = "SELECT schemaname, relname AS tablename, indexrelname AS indexname, idx_scan, " +
                "pg_size_pretty(pg_relation_size(indexrelid)) as index_size, " +
                "pg_relation_size(indexrelid) / (1024.0 * 1024.0) as size_mb " +
                "FROM pg_stat_user_indexes WHERE idx_scan = 0 AND indexrelname NOT LIKE '%_pkey' " +
                "ORDER BY pg_relation_size(indexrelid) DESC LIMIT 20";

        double totalWastedSpace = 0.0;
        int totalIndexes = 0;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(unusedIndexQuery)) {
            while (rs.next()) {
                String schema = rs.getString("schemaname");
                String table = rs.getString("tablename");
                String indexName = rs.getString("indexname");
                long indexScans = rs.getLong("idx_scan");
                double sizeMB = rs.getDouble("size_mb");

                totalWastedSpace += sizeMB;
                totalIndexes++;

                String suggestion = String.format("Index has never been used (0 scans). Consider dropping to save %.2f MB.", sizeMB);
                String dropStatement = String.format("DROP INDEX CONCURRENTLY %s.%s;", schema, indexName);

                unusedIndexes.add(new IndexRecommendationDTO.UnusedIndex(
                        schema, table, indexName, indexScans, sizeMB, suggestion, dropStatement
                ));
            }
        }

        result.setMissingIndexes(missingIndexes);
        result.setUnusedIndexes(unusedIndexes);
        result.setTotalIndexesAnalyzed(totalIndexes);
        result.setTotalWastedSpaceMB(totalWastedSpace);

        logger.info("Index analysis completed: {} missing, {} unused indexes", missingIndexes.size(), unusedIndexes.size());
        return result;
    }

    public QueryOptimizationDTO analyzeQueries(String connectionId, String username) throws Exception {
        Connection conn = connectionService.getConnectionForAnalytics(connectionId, username);
        String dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (!dbType.contains("postgres")) {
            throw new IllegalArgumentException("Query analysis currently only supports PostgreSQL");
        }

        QueryOptimizationDTO result = new QueryOptimizationDTO();
        List<QueryOptimizationDTO.SlowQuery> slowQueries = new ArrayList<>();

        boolean usesNewColumnNames = false;
        try (Statement checkStmt = conn.createStatement()) {
            checkStmt.executeQuery("SELECT mean_exec_time FROM pg_stat_statements LIMIT 0");
            usesNewColumnNames = true;
        } catch (SQLException e) {
            usesNewColumnNames = false;
        }

        String meanTimeCol = usesNewColumnNames ? "mean_exec_time" : "mean_time";
        String totalTimeCol = usesNewColumnNames ? "total_exec_time" : "total_time";

        String slowQuerySQL = "SELECT query, calls, " +
                meanTimeCol + " as mean_time, " +
                totalTimeCol + " as total_time, " +
                "(" + meanTimeCol + " * calls) as total_impact " +
                "FROM pg_stat_statements " +
                "WHERE " + meanTimeCol + " > 100 AND query NOT LIKE '%pg_stat%' " +
                "ORDER BY " + meanTimeCol + " DESC LIMIT 20";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(slowQuerySQL)) {
            double totalTime = 0.0;
            int count = 0;

            while (rs.next()) {
                String queryText = rs.getString("query");
                double meanTime = rs.getDouble("mean_time");
                long calls = rs.getLong("calls");
                double totalExecTime = rs.getDouble("total_time");

                count++;
                totalTime += meanTime;

                String priority = meanTime > 1000 ? "HIGH" : meanTime > 500 ? "MEDIUM" : "LOW";
                String suggestion = meanTime > 1000
                        ? "Critical: Review execution plan with EXPLAIN ANALYZE and add missing indexes"
                        : "Review query and consider adding indexes on filtered columns";

                if (queryText.length() > 200) {
                    queryText = queryText.substring(0, 197) + "...";
                }

                slowQueries.add(new QueryOptimizationDTO.SlowQuery(
                        queryText, meanTime, calls, totalExecTime, suggestion, priority
                ));
            }

            result.setSlowQueries(slowQueries);
            result.setTotalQueriesAnalyzed(count);
            result.setAvgQueryTimeMs(count > 0 ? totalTime / count : 0.0);

        } catch (SQLException e) {
            if (e.getMessage().contains("pg_stat_statements")) {
                logger.warn("pg_stat_statements extension not enabled. Query analysis unavailable.");
                result.setSlowQueries(new ArrayList<>());
                result.setTotalQueriesAnalyzed(0);
                result.setAvgQueryTimeMs(0.0);
            } else {
                throw e;
            }
        }

        logger.info("Query analysis completed: {} slow queries found", slowQueries.size());
        return result;
    }

    public ConfigOptimizationDTO analyzeConfig(String connectionId, String username) throws Exception {
        Connection conn = connectionService.getConnectionForAnalytics(connectionId, username);
        String dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (!dbType.contains("postgres")) {
            throw new IllegalArgumentException("Config analysis currently only supports PostgreSQL");
        }

        ConfigOptimizationDTO result = new ConfigOptimizationDTO();
        List<ConfigOptimizationDTO.ConfigSuggestion> suggestions = new ArrayList<>();

        int totalMemoryMB = 8192;
        int cpuCores = 4;
        int maxConnections = 100;
        int currentConnections = 0;

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT current_setting('max_connections')::int");
            if (rs.next()) {
                maxConnections = rs.getInt(1);
            }

            rs = stmt.executeQuery("SELECT count(*) FROM pg_stat_activity");
            if (rs.next()) {
                currentConnections = rs.getInt(1);
            }
        }

        String configQuery = "SELECT name, setting, unit FROM pg_settings " +
                "WHERE name IN ('shared_buffers', 'work_mem', 'maintenance_work_mem', " +
                "'effective_cache_size', 'max_connections')";

        Map<String, String> currentConfig = new HashMap<>();

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(configQuery)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String setting = rs.getString("setting");
                String unit = rs.getString("unit");

                String value = unit != null && !unit.isEmpty() ? setting + unit : setting;
                currentConfig.put(name, value);
            }
        }

        String sharedBuffers = currentConfig.getOrDefault("shared_buffers", "128MB");
        int sharedBuffersMB = parseMemoryToMB(sharedBuffers);
        int suggestedSharedBuffers = totalMemoryMB / 4;
        if (sharedBuffersMB < suggestedSharedBuffers * 0.8) {
            suggestions.add(new ConfigOptimizationDTO.ConfigSuggestion(
                    "shared_buffers",
                    sharedBuffers,
                    suggestedSharedBuffers + "MB",
                    "Should be ~25% of total RAM for optimal performance",
                    "HIGH",
                    "Significant performance improvement for read-heavy workloads"
            ));
        }

        String workMem = currentConfig.getOrDefault("work_mem", "4MB");
        int workMemMB = parseMemoryToMB(workMem);
        int suggestedWorkMem = Math.max(4, totalMemoryMB / (maxConnections * 3));
        if (workMemMB < suggestedWorkMem * 0.5) {
            suggestions.add(new ConfigOptimizationDTO.ConfigSuggestion(
                    "work_mem",
                    workMem,
                    suggestedWorkMem + "MB",
                    "Based on total memory and max connections. Affects sort and hash operations",
                    "MEDIUM",
                    "Improves sorting and hashing performance"
            ));
        }

        String effectiveCache = currentConfig.getOrDefault("effective_cache_size", "4GB");
        int effectiveCacheMB = parseMemoryToMB(effectiveCache);
        int suggestedEffectiveCache = (totalMemoryMB * 3) / 4;
        if (effectiveCacheMB < suggestedEffectiveCache * 0.7) {
            suggestions.add(new ConfigOptimizationDTO.ConfigSuggestion(
                    "effective_cache_size",
                    effectiveCache,
                    suggestedEffectiveCache + "MB",
                    "Should be ~75% of total RAM. Helps query planner make better decisions",
                    "MEDIUM",
                    "Better query planning and index usage decisions"
            ));
        }

        result.setSuggestions(suggestions);
        result.setTotalMemoryMB(totalMemoryMB);
        result.setCpuCores(cpuCores);
        result.setCurrentConnections(currentConnections);
        result.setMaxConnections(maxConnections);

        logger.info("Config analysis completed: {} suggestions generated", suggestions.size());
        return result;
    }

    public VacuumAnalysisDTO analyzeVacuum(String connectionId, String username) throws Exception {
        Connection conn = connectionService.getConnectionForAnalytics(connectionId, username);
        String dbType = conn.getMetaData().getDatabaseProductName().toLowerCase();

        if (!dbType.contains("postgres")) {
            throw new IllegalArgumentException("Vacuum analysis currently only supports PostgreSQL");
        }

        VacuumAnalysisDTO result = new VacuumAnalysisDTO();
        List<VacuumAnalysisDTO.TableVacuumStatus> tablesNeedingVacuum = new ArrayList<>();

        String vacuumQuery = "SELECT schemaname, relname AS tablename, n_live_tup, n_dead_tup, " +
                "CASE WHEN n_live_tup > 0 THEN (n_dead_tup::float / n_live_tup::float) * 100 ELSE 0 END as dead_tuple_percent, " +
                "last_vacuum, last_autovacuum FROM pg_stat_user_tables " +
                "WHERE n_dead_tup > 100 ORDER BY dead_tuple_percent DESC LIMIT 30";

        long totalDeadTuples = 0;
        int tablesAnalyzed = 0;

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(vacuumQuery)) {
            while (rs.next()) {
                String schema = rs.getString("schemaname");
                String table = rs.getString("tablename");
                long liveTuples = rs.getLong("n_live_tup");
                long deadTuples = rs.getLong("n_dead_tup");
                double deadPercent = rs.getDouble("dead_tuple_percent");

                totalDeadTuples += deadTuples;
                tablesAnalyzed++;

                String lastVacuum = rs.getTimestamp("last_vacuum") != null
                        ? rs.getTimestamp("last_vacuum").toString()
                        : "Never";
                String lastAutoVacuum = rs.getTimestamp("last_autovacuum") != null
                        ? rs.getTimestamp("last_autovacuum").toString()
                        : "Never";

                String priority = deadPercent > 20 ? "HIGH" : deadPercent > 10 ? "MEDIUM" : "LOW";
                String suggestion = deadPercent > 20
                        ? String.format("CRITICAL: %.1f%% dead tuples. Run VACUUM ANALYZE immediately", deadPercent)
                        : String.format("%.1f%% dead tuples. Consider running VACUUM", deadPercent);

                tablesNeedingVacuum.add(new VacuumAnalysisDTO.TableVacuumStatus(
                        schema, table, liveTuples, deadTuples, deadPercent,
                        lastVacuum, lastAutoVacuum, suggestion, priority
                ));
            }
        }

        String overallHealth = totalDeadTuples > 1000000 ? "CRITICAL"
                : totalDeadTuples > 100000 ? "WARNING"
                : totalDeadTuples > 10000 ? "FAIR"
                : "GOOD";

        result.setTablesNeedingVacuum(tablesNeedingVacuum);
        result.setTotalDeadTuples(totalDeadTuples);
        result.setTablesAnalyzed(tablesAnalyzed);
        result.setOverallHealth(overallHealth);

        logger.info("Vacuum analysis completed: {} tables need attention, {} total dead tuples",
                tablesNeedingVacuum.size(), totalDeadTuples);
        return result;
    }

    private int parseMemoryToMB(String memoryString) {
        if (memoryString == null || memoryString.isEmpty()) {
            return 0;
        }

        String value = memoryString.replaceAll("[^0-9]", "");
        if (value.isEmpty()) {
            return 0;
        }

        int number = Integer.parseInt(value);
        String upper = memoryString.toUpperCase();

        if (upper.contains("GB")) {
            return number * 1024;
        } else if (upper.contains("MB")) {
            return number;
        } else if (upper.contains("KB")) {
            return number / 1024;
        } else if (upper.contains("8KB")) {
            return (number * 8) / 1024;
        }

        return number;
    }

    public byte[] exportOptimizationReport(String connectionId, String username) throws Exception {
        logger.info("Generating optimization report for connection: {} (user: {})", connectionId, username);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet summarySheet = workbook.createSheet("Summary");
            org.apache.poi.ss.usermodel.Sheet indexSheet = workbook.createSheet("Index Analysis");
            org.apache.poi.ss.usermodel.Sheet querySheet = workbook.createSheet("Query Optimization");
            org.apache.poi.ss.usermodel.Sheet configSheet = workbook.createSheet("Configuration");
            org.apache.poi.ss.usermodel.Sheet vacuumSheet = workbook.createSheet("VACUUM Analysis");

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            IndexRecommendationDTO indexAnalysis = analyzeIndexes(connectionId, username);
            QueryOptimizationDTO queryAnalysis = analyzeQueries(connectionId, username);
            ConfigOptimizationDTO configAnalysis = analyzeConfig(connectionId, username);
            VacuumAnalysisDTO vacuumAnalysis = analyzeVacuum(connectionId, username);

            if (indexAnalysis == null || queryAnalysis == null || configAnalysis == null || vacuumAnalysis == null) {
                throw new IllegalStateException("Failed to retrieve optimization analysis data");
            }

            createSummarySheet(summarySheet, headerStyle, indexAnalysis, queryAnalysis, configAnalysis, vacuumAnalysis);
            createIndexSheet(indexSheet, headerStyle, indexAnalysis);
            createQuerySheet(querySheet, headerStyle, queryAnalysis);
            createConfigSheet(configSheet, headerStyle, configAnalysis);
            createVacuumSheet(vacuumSheet, headerStyle, vacuumAnalysis);

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                for (int j = 0; j < 10; j++) {
                    sheet.autoSizeColumn(j);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createSummarySheet(org.apache.poi.ss.usermodel.Sheet sheet, org.apache.poi.ss.usermodel.CellStyle headerStyle,
                                    IndexRecommendationDTO indexAnalysis, QueryOptimizationDTO queryAnalysis,
                                    ConfigOptimizationDTO configAnalysis, VacuumAnalysisDTO vacuumAnalysis) {
        int rowNum = 0;
        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("PostgreSQL Optimization Report - Summary");
        titleCell.setCellStyle(headerStyle);

        rowNum++;
        org.apache.poi.ss.usermodel.Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Generated:");
        dateRow.createCell(1).setCellValue(new java.util.Date().toString());

        rowNum++;
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Analysis Type");
        headerRow.createCell(1).setCellValue("Status");
        headerRow.createCell(2).setCellValue("Recommendations");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        headerRow.getCell(2).setCellStyle(headerStyle);

        org.apache.poi.ss.usermodel.Row indexRow = sheet.createRow(rowNum++);
        indexRow.createCell(0).setCellValue("Index Analysis");
        indexRow.createCell(1).setCellValue(indexAnalysis.getMissingIndexes().size() > 5 ? "NEEDS ATTENTION" : "GOOD");
        indexRow.createCell(2).setCellValue(
                indexAnalysis.getMissingIndexes().size() + " missing, " +
                        indexAnalysis.getUnusedIndexes().size() + " unused"
        );

        org.apache.poi.ss.usermodel.Row queryRow = sheet.createRow(rowNum++);
        queryRow.createCell(0).setCellValue("Query Optimization");
        queryRow.createCell(1).setCellValue(queryAnalysis.getSlowQueries().size() > 10 ? "NEEDS ATTENTION" : "GOOD");
        queryRow.createCell(2).setCellValue(queryAnalysis.getSlowQueries().size() + " slow queries found");

        org.apache.poi.ss.usermodel.Row configRow = sheet.createRow(rowNum++);
        configRow.createCell(0).setCellValue("Configuration Tuning");
        configRow.createCell(1).setCellValue(configAnalysis.getSuggestions().size() > 3 ? "NEEDS TUNING" : "GOOD");
        configRow.createCell(2).setCellValue(configAnalysis.getSuggestions().size() + " recommendations");

        org.apache.poi.ss.usermodel.Row vacuumRow = sheet.createRow(rowNum++);
        vacuumRow.createCell(0).setCellValue("VACUUM Analysis");
        vacuumRow.createCell(1).setCellValue(vacuumAnalysis.getOverallHealth());
        vacuumRow.createCell(2).setCellValue(
                vacuumAnalysis.getTablesNeedingVacuum().size() + " tables need attention"
        );
    }

    private void createIndexSheet(org.apache.poi.ss.usermodel.Sheet sheet, org.apache.poi.ss.usermodel.CellStyle headerStyle,
                                  IndexRecommendationDTO analysis) {
        int rowNum = 0;

        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Missing Indexes (High Sequential Scans)");
        titleRow.getCell(0).setCellStyle(headerStyle);

        rowNum++;
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Schema");
        headerRow.createCell(1).setCellValue("Table");
        headerRow.createCell(2).setCellValue("Sequential Scans");
        headerRow.createCell(3).setCellValue("Index Scans");
        headerRow.createCell(4).setCellValue("Rows Read");
        headerRow.createCell(5).setCellValue("Priority");
        headerRow.createCell(6).setCellValue("Suggestion");
        for (int i = 0; i < 7; i++) headerRow.getCell(i).setCellStyle(headerStyle);

        if (analysis.getMissingIndexes() != null) {
            for (IndexRecommendationDTO.MissingIndex idx : analysis.getMissingIndexes()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx.getSchema() != null ? idx.getSchema() : "");
                row.createCell(1).setCellValue(idx.getTable() != null ? idx.getTable() : "");
                row.createCell(2).setCellValue(idx.getSequentialScans());
                row.createCell(3).setCellValue(idx.getIndexScans());
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue(idx.getPriority() != null ? idx.getPriority() : "");
                row.createCell(6).setCellValue(idx.getSuggestion() != null ? idx.getSuggestion() : "");
            }
        }

        rowNum += 2;
        org.apache.poi.ss.usermodel.Row unusedTitleRow = sheet.createRow(rowNum++);
        unusedTitleRow.createCell(0).setCellValue("Unused Indexes (Wasting Disk Space)");
        unusedTitleRow.getCell(0).setCellStyle(headerStyle);

        rowNum++;
        org.apache.poi.ss.usermodel.Row unusedHeaderRow = sheet.createRow(rowNum++);
        unusedHeaderRow.createCell(0).setCellValue("Schema");
        unusedHeaderRow.createCell(1).setCellValue("Table");
        unusedHeaderRow.createCell(2).setCellValue("Index Name");
        unusedHeaderRow.createCell(3).setCellValue("Size (MB)");
        unusedHeaderRow.createCell(4).setCellValue("Priority");
        unusedHeaderRow.createCell(5).setCellValue("Suggestion");
        for (int i = 0; i < 6; i++) unusedHeaderRow.getCell(i).setCellStyle(headerStyle);

        if (analysis.getUnusedIndexes() != null) {
            for (IndexRecommendationDTO.UnusedIndex idx : analysis.getUnusedIndexes()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx.getSchema() != null ? idx.getSchema() : "");
                row.createCell(1).setCellValue(idx.getTable() != null ? idx.getTable() : "");
                row.createCell(2).setCellValue(idx.getIndexName() != null ? idx.getIndexName() : "");
                row.createCell(3).setCellValue(idx.getSizeMB());
                row.createCell(4).setCellValue(idx.getSizeMB() > 100 ? "HIGH" : idx.getSizeMB() > 10 ? "MEDIUM" : "LOW");
                row.createCell(5).setCellValue(idx.getSuggestion() != null ? idx.getSuggestion() : "");
            }
        }
    }

    private void createQuerySheet(org.apache.poi.ss.usermodel.Sheet sheet, org.apache.poi.ss.usermodel.CellStyle headerStyle,
                                  QueryOptimizationDTO analysis) {
        int rowNum = 0;

        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Slow Queries (>100ms)");
        titleRow.getCell(0).setCellStyle(headerStyle);

        rowNum++;
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Query (First 100 chars)");
        headerRow.createCell(1).setCellValue("Calls");
        headerRow.createCell(2).setCellValue("Avg Time (ms)");
        headerRow.createCell(3).setCellValue("Total Time (s)");
        headerRow.createCell(4).setCellValue("Priority");
        headerRow.createCell(5).setCellValue("Recommendation");
        for (int i = 0; i < 6; i++) headerRow.getCell(i).setCellStyle(headerStyle);

        if (analysis.getSlowQueries() != null) {
            for (QueryOptimizationDTO.SlowQuery query : analysis.getSlowQueries()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                String queryText = query.getQueryText() != null ? query.getQueryText() : "";
                String truncatedQuery = queryText.length() > 100
                        ? queryText.substring(0, 100) + "..."
                        : queryText;
                row.createCell(0).setCellValue(truncatedQuery);
                row.createCell(1).setCellValue(query.getCalls());
                row.createCell(2).setCellValue(query.getMeanExecTimeMs());
                row.createCell(3).setCellValue(query.getTotalExecTimeMs() / 1000.0);
                row.createCell(4).setCellValue(query.getPriority() != null ? query.getPriority() : "");
                row.createCell(5).setCellValue(query.getSuggestion() != null ? query.getSuggestion() : "");
            }
        }
    }

    private void createConfigSheet(org.apache.poi.ss.usermodel.Sheet sheet, org.apache.poi.ss.usermodel.CellStyle headerStyle,
                                   ConfigOptimizationDTO analysis) {
        int rowNum = 0;

        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Configuration Recommendations");
        titleRow.getCell(0).setCellStyle(headerStyle);

        rowNum++;
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Parameter");
        headerRow.createCell(2).setCellValue("Current Value");
        headerRow.createCell(3).setCellValue("Suggested Value");
        headerRow.createCell(4).setCellValue("Priority");
        headerRow.createCell(5).setCellValue("Description");
        for (int i = 0; i < 6; i++) headerRow.getCell(i).setCellStyle(headerStyle);

        if (analysis.getSuggestions() != null) {
            for (ConfigOptimizationDTO.ConfigSuggestion rec : analysis.getSuggestions()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("Configuration");
                row.createCell(1).setCellValue(rec.getParameter() != null ? rec.getParameter() : "");
                row.createCell(2).setCellValue(rec.getCurrentValue() != null ? rec.getCurrentValue() : "");
                row.createCell(3).setCellValue(rec.getSuggestedValue() != null ? rec.getSuggestedValue() : "");
                row.createCell(4).setCellValue(rec.getPriority() != null ? rec.getPriority() : "");
                row.createCell(5).setCellValue(rec.getReason() != null ? rec.getReason() : "");
            }
        }
    }

    private void createVacuumSheet(org.apache.poi.ss.usermodel.Sheet sheet, org.apache.poi.ss.usermodel.CellStyle headerStyle,
                                   VacuumAnalysisDTO analysis) {
        int rowNum = 0;

        org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Tables Needing VACUUM");
        titleRow.getCell(0).setCellStyle(headerStyle);

        rowNum++;
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Schema");
        headerRow.createCell(1).setCellValue("Table");
        headerRow.createCell(2).setCellValue("Live Tuples");
        headerRow.createCell(3).setCellValue("Dead Tuples");
        headerRow.createCell(4).setCellValue("Dead %");
        headerRow.createCell(5).setCellValue("Last Vacuum");
        headerRow.createCell(6).setCellValue("Last Auto Vacuum");
        headerRow.createCell(7).setCellValue("Priority");
        headerRow.createCell(8).setCellValue("Suggestion");
        for (int i = 0; i < 9; i++) headerRow.getCell(i).setCellStyle(headerStyle);

        if (analysis.getTablesNeedingVacuum() != null) {
            for (VacuumAnalysisDTO.TableVacuumStatus table : analysis.getTablesNeedingVacuum()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(table.getSchema() != null ? table.getSchema() : "");
                row.createCell(1).setCellValue(table.getTable() != null ? table.getTable() : "");
                row.createCell(2).setCellValue(table.getLiveTuples());
                row.createCell(3).setCellValue(table.getDeadTuples());
                row.createCell(4).setCellValue(String.format("%.1f%%", table.getDeadTuplePercent()));
                row.createCell(5).setCellValue(table.getLastVacuum() != null ? table.getLastVacuum() : "");
                row.createCell(6).setCellValue(table.getLastAutoVacuum() != null ? table.getLastAutoVacuum() : "");
                row.createCell(7).setCellValue(table.getPriority() != null ? table.getPriority() : "");
                row.createCell(8).setCellValue(table.getSuggestion() != null ? table.getSuggestion() : "");
            }
        }
    }
}
