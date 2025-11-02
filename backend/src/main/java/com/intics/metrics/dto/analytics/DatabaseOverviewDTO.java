package com.intics.metrics.dto.analytics;

import java.util.List;

public class DatabaseOverviewDTO {
    private String databaseName;
    private int totalSchemas;
    private int totalTables;
    private long totalRows;
    private double totalSizeMB;
    private List<SchemaStats> schemaStats;
    private List<TableSizeInfo> largestTables;

    public static class SchemaStats {
        private String schemaName;
        private int tableCount;
        private double sizeMB;

        public SchemaStats() {}

        public SchemaStats(String schemaName, int tableCount, double sizeMB) {
            this.schemaName = schemaName;
            this.tableCount = tableCount;
            this.sizeMB = sizeMB;
        }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public int getTableCount() { return tableCount; }
        public void setTableCount(int tableCount) { this.tableCount = tableCount; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
    }

    public static class TableSizeInfo {
        private String schemaName;
        private String tableName;
        private long rowCount;
        private double sizeMB;

        public TableSizeInfo() {}

        public TableSizeInfo(String schemaName, String tableName, long rowCount, double sizeMB) {
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.rowCount = rowCount;
            this.sizeMB = sizeMB;
        }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public long getRowCount() { return rowCount; }
        public void setRowCount(long rowCount) { this.rowCount = rowCount; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
    }

    public DatabaseOverviewDTO() {}

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public int getTotalSchemas() { return totalSchemas; }
    public void setTotalSchemas(int totalSchemas) { this.totalSchemas = totalSchemas; }
    public int getTotalTables() { return totalTables; }
    public void setTotalTables(int totalTables) { this.totalTables = totalTables; }
    public long getTotalRows() { return totalRows; }
    public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
    public double getTotalSizeMB() { return totalSizeMB; }
    public void setTotalSizeMB(double totalSizeMB) { this.totalSizeMB = totalSizeMB; }
    public List<SchemaStats> getSchemaStats() { return schemaStats; }
    public void setSchemaStats(List<SchemaStats> schemaStats) { this.schemaStats = schemaStats; }
    public List<TableSizeInfo> getLargestTables() { return largestTables; }
    public void setLargestTables(List<TableSizeInfo> largestTables) { this.largestTables = largestTables; }
}
