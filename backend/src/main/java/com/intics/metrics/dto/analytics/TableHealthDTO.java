package com.intics.metrics.dto.analytics;

import java.util.List;

public class TableHealthDTO {
    private List<UnusedTable> unusedTables;
    private List<EmptyTable> emptyTables;
    private List<TableStats> tableStatistics;

    public static class UnusedTable {
        private String schemaName;
        private String tableName;
        private long rowCount;
        private double sizeMB;
        private String reason;

        public UnusedTable() {}

        public UnusedTable(String schemaName, String tableName, long rowCount, double sizeMB, String reason) {
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.rowCount = rowCount;
            this.sizeMB = sizeMB;
            this.reason = reason;
        }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public long getRowCount() { return rowCount; }
        public void setRowCount(long rowCount) { this.rowCount = rowCount; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class EmptyTable {
        private String schemaName;
        private String tableName;
        private double sizeMB;

        public EmptyTable() {}

        public EmptyTable(String schemaName, String tableName, double sizeMB) {
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.sizeMB = sizeMB;
        }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
    }

    public static class TableStats {
        private String schemaName;
        private String tableName;
        private long rowCount;
        private int columnCount;
        private double sizeMB;
        private String healthStatus;

        public TableStats() {}

        public TableStats(String schemaName, String tableName, long rowCount, int columnCount, double sizeMB, String healthStatus) {
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.rowCount = rowCount;
            this.columnCount = columnCount;
            this.sizeMB = sizeMB;
            this.healthStatus = healthStatus;
        }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public long getRowCount() { return rowCount; }
        public void setRowCount(long rowCount) { this.rowCount = rowCount; }
        public int getColumnCount() { return columnCount; }
        public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
        public String getHealthStatus() { return healthStatus; }
        public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    }

    public TableHealthDTO() {}

    public List<UnusedTable> getUnusedTables() { return unusedTables; }
    public void setUnusedTables(List<UnusedTable> unusedTables) { this.unusedTables = unusedTables; }
    public List<EmptyTable> getEmptyTables() { return emptyTables; }
    public void setEmptyTables(List<EmptyTable> emptyTables) { this.emptyTables = emptyTables; }
    public List<TableStats> getTableStatistics() { return tableStatistics; }
    public void setTableStatistics(List<TableStats> tableStatistics) { this.tableStatistics = tableStatistics; }
}
