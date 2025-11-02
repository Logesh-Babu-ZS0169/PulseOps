package com.intics.metrics.dto.analytics;

import java.util.List;

public class SchemaRelationshipsDTO {
    private List<Relationship> relationships;
    private List<IsolatedTable> isolatedTables;
    private int totalRelationships;
    private int connectedTables;
    private int isolatedTableCount;

    public static class Relationship {
        private String sourceSchema;
        private String sourceTable;
        private String sourceColumn;
        private String targetSchema;
        private String targetTable;
        private String targetColumn;
        private String constraintName;

        public Relationship() {}

        public Relationship(String sourceSchema, String sourceTable, String sourceColumn,
                          String targetSchema, String targetTable, String targetColumn,
                          String constraintName) {
            this.sourceSchema = sourceSchema;
            this.sourceTable = sourceTable;
            this.sourceColumn = sourceColumn;
            this.targetSchema = targetSchema;
            this.targetTable = targetTable;
            this.targetColumn = targetColumn;
            this.constraintName = constraintName;
        }

        public String getSourceSchema() { return sourceSchema; }
        public void setSourceSchema(String sourceSchema) { this.sourceSchema = sourceSchema; }
        public String getSourceTable() { return sourceTable; }
        public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
        public String getSourceColumn() { return sourceColumn; }
        public void setSourceColumn(String sourceColumn) { this.sourceColumn = sourceColumn; }
        public String getTargetSchema() { return targetSchema; }
        public void setTargetSchema(String targetSchema) { this.targetSchema = targetSchema; }
        public String getTargetTable() { return targetTable; }
        public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
        public String getTargetColumn() { return targetColumn; }
        public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }
        public String getConstraintName() { return constraintName; }
        public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
    }

    public static class IsolatedTable {
        private String schemaName;
        private String tableName;
        private long rowCount;
        private String reason;

        public IsolatedTable() {}

        public IsolatedTable(String schemaName, String tableName, long rowCount, String reason) {
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.rowCount = rowCount;
            this.reason = reason;
        }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public long getRowCount() { return rowCount; }
        public void setRowCount(long rowCount) { this.rowCount = rowCount; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public SchemaRelationshipsDTO() {}

    public List<Relationship> getRelationships() { return relationships; }
    public void setRelationships(List<Relationship> relationships) { this.relationships = relationships; }
    public List<IsolatedTable> getIsolatedTables() { return isolatedTables; }
    public void setIsolatedTables(List<IsolatedTable> isolatedTables) { this.isolatedTables = isolatedTables; }
    public int getTotalRelationships() { return totalRelationships; }
    public void setTotalRelationships(int totalRelationships) { this.totalRelationships = totalRelationships; }
    public int getConnectedTables() { return connectedTables; }
    public void setConnectedTables(int connectedTables) { this.connectedTables = connectedTables; }
    public int getIsolatedTableCount() { return isolatedTableCount; }
    public void setIsolatedTableCount(int isolatedTableCount) { this.isolatedTableCount = isolatedTableCount; }
}
