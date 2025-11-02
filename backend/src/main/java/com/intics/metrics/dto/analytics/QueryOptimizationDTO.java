package com.intics.metrics.dto.analytics;

import java.util.List;

public class QueryOptimizationDTO {
    
    public static class SlowQuery {
        private String queryText;
        private double meanExecTimeMs;
        private long calls;
        private double totalExecTimeMs;
        private String suggestion;
        private String priority;
        
        public SlowQuery() {}
        
        public SlowQuery(String queryText, double meanExecTimeMs, long calls, 
                        double totalExecTimeMs, String suggestion, String priority) {
            this.queryText = queryText;
            this.meanExecTimeMs = meanExecTimeMs;
            this.calls = calls;
            this.totalExecTimeMs = totalExecTimeMs;
            this.suggestion = suggestion;
            this.priority = priority;
        }
        
        // Getters and setters
        public String getQueryText() { return queryText; }
        public void setQueryText(String queryText) { this.queryText = queryText; }
        public double getMeanExecTimeMs() { return meanExecTimeMs; }
        public void setMeanExecTimeMs(double meanExecTimeMs) { this.meanExecTimeMs = meanExecTimeMs; }
        public long getCalls() { return calls; }
        public void setCalls(long calls) { this.calls = calls; }
        public double getTotalExecTimeMs() { return totalExecTimeMs; }
        public void setTotalExecTimeMs(double totalExecTimeMs) { this.totalExecTimeMs = totalExecTimeMs; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }
    
    private List<SlowQuery> slowQueries;
    private int totalQueriesAnalyzed;
    private double avgQueryTimeMs;
    
    public QueryOptimizationDTO() {}
    
    // Getters and setters
    public List<SlowQuery> getSlowQueries() { return slowQueries; }
    public void setSlowQueries(List<SlowQuery> slowQueries) { this.slowQueries = slowQueries; }
    public int getTotalQueriesAnalyzed() { return totalQueriesAnalyzed; }
    public void setTotalQueriesAnalyzed(int totalQueriesAnalyzed) { this.totalQueriesAnalyzed = totalQueriesAnalyzed; }
    public double getAvgQueryTimeMs() { return avgQueryTimeMs; }
    public void setAvgQueryTimeMs(double avgQueryTimeMs) { this.avgQueryTimeMs = avgQueryTimeMs; }
}
