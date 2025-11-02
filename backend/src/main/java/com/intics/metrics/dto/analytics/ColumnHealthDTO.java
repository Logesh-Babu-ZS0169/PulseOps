package com.intics.metrics.dto.analytics;

import java.util.List;

public class ColumnHealthDTO {
    private List<ProblematicColumn> nullHeavyColumns;
    private List<ProblematicColumn> redundantColumns;
    private int totalColumnsAnalyzed;
    private int healthyColumns;
    private int problematicColumns;

    public static class ProblematicColumn {
        private String schemaName;
        private String tableName;
        private String columnName;
        private String dataType;
        private double nullPercentage;
        private String issue;
        private String recommendation;

        public ProblematicColumn() {}

        public ProblematicColumn(String schemaName, String tableName, String columnName, 
                                String dataType, double nullPercentage, String issue, String recommendation) {
            this.schemaName = schemaName;
            this.tableName = tableName;
            this.columnName = columnName;
            this.dataType = dataType;
            this.nullPercentage = nullPercentage;
            this.issue = issue;
            this.recommendation = recommendation;
        }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public double getNullPercentage() { return nullPercentage; }
        public void setNullPercentage(double nullPercentage) { this.nullPercentage = nullPercentage; }
        public String getIssue() { return issue; }
        public void setIssue(String issue) { this.issue = issue; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    public ColumnHealthDTO() {}

    public List<ProblematicColumn> getNullHeavyColumns() { return nullHeavyColumns; }
    public void setNullHeavyColumns(List<ProblematicColumn> nullHeavyColumns) { this.nullHeavyColumns = nullHeavyColumns; }
    public List<ProblematicColumn> getRedundantColumns() { return redundantColumns; }
    public void setRedundantColumns(List<ProblematicColumn> redundantColumns) { this.redundantColumns = redundantColumns; }
    public int getTotalColumnsAnalyzed() { return totalColumnsAnalyzed; }
    public void setTotalColumnsAnalyzed(int totalColumnsAnalyzed) { this.totalColumnsAnalyzed = totalColumnsAnalyzed; }
    public int getHealthyColumns() { return healthyColumns; }
    public void setHealthyColumns(int healthyColumns) { this.healthyColumns = healthyColumns; }
    public int getProblematicColumns() { return problematicColumns; }
    public void setProblematicColumns(int problematicColumns) { this.problematicColumns = problematicColumns; }
}
