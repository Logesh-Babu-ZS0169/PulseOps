package com.intics.metrics.dto.analytics;

import java.util.List;

public class VacuumAnalysisDTO {
    
    public static class TableVacuumStatus {
        private String schema;
        private String table;
        private long liveTuples;
        private long deadTuples;
        private double deadTuplePercent;
        private String lastVacuum;
        private String lastAutoVacuum;
        private String suggestion;
        private String priority;
        
        public TableVacuumStatus() {}
        
        public TableVacuumStatus(String schema, String table, long liveTuples, long deadTuples,
                                double deadTuplePercent, String lastVacuum, String lastAutoVacuum,
                                String suggestion, String priority) {
            this.schema = schema;
            this.table = table;
            this.liveTuples = liveTuples;
            this.deadTuples = deadTuples;
            this.deadTuplePercent = deadTuplePercent;
            this.lastVacuum = lastVacuum;
            this.lastAutoVacuum = lastAutoVacuum;
            this.suggestion = suggestion;
            this.priority = priority;
        }
        
        // Getters and setters
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public String getTable() { return table; }
        public void setTable(String table) { this.table = table; }
        public long getLiveTuples() { return liveTuples; }
        public void setLiveTuples(long liveTuples) { this.liveTuples = liveTuples; }
        public long getDeadTuples() { return deadTuples; }
        public void setDeadTuples(long deadTuples) { this.deadTuples = deadTuples; }
        public double getDeadTuplePercent() { return deadTuplePercent; }
        public void setDeadTuplePercent(double deadTuplePercent) { this.deadTuplePercent = deadTuplePercent; }
        public String getLastVacuum() { return lastVacuum; }
        public void setLastVacuum(String lastVacuum) { this.lastVacuum = lastVacuum; }
        public String getLastAutoVacuum() { return lastAutoVacuum; }
        public void setLastAutoVacuum(String lastAutoVacuum) { this.lastAutoVacuum = lastAutoVacuum; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }
    
    private List<TableVacuumStatus> tablesNeedingVacuum;
    private long totalDeadTuples;
    private int tablesAnalyzed;
    private String overallHealth;
    
    public VacuumAnalysisDTO() {}
    
    // Getters and setters
    public List<TableVacuumStatus> getTablesNeedingVacuum() { return tablesNeedingVacuum; }
    public void setTablesNeedingVacuum(List<TableVacuumStatus> tablesNeedingVacuum) { this.tablesNeedingVacuum = tablesNeedingVacuum; }
    public long getTotalDeadTuples() { return totalDeadTuples; }
    public void setTotalDeadTuples(long totalDeadTuples) { this.totalDeadTuples = totalDeadTuples; }
    public int getTablesAnalyzed() { return tablesAnalyzed; }
    public void setTablesAnalyzed(int tablesAnalyzed) { this.tablesAnalyzed = tablesAnalyzed; }
    public String getOverallHealth() { return overallHealth; }
    public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
}
