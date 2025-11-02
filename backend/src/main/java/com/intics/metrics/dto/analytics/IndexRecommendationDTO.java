package com.intics.metrics.dto.analytics;

import java.util.List;

public class IndexRecommendationDTO {
    
    public static class MissingIndex {
        private String schema;
        private String table;
        private long sequentialScans;
        private long indexScans;
        private String suggestion;
        private String proposedIndex;
        private String priority;
        
        public MissingIndex() {}
        
        public MissingIndex(String schema, String table, long sequentialScans, long indexScans, 
                          String suggestion, String proposedIndex, String priority) {
            this.schema = schema;
            this.table = table;
            this.sequentialScans = sequentialScans;
            this.indexScans = indexScans;
            this.suggestion = suggestion;
            this.proposedIndex = proposedIndex;
            this.priority = priority;
        }
        
        // Getters and setters
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public String getTable() { return table; }
        public void setTable(String table) { this.table = table; }
        public long getSequentialScans() { return sequentialScans; }
        public void setSequentialScans(long sequentialScans) { this.sequentialScans = sequentialScans; }
        public long getIndexScans() { return indexScans; }
        public void setIndexScans(long indexScans) { this.indexScans = indexScans; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public String getProposedIndex() { return proposedIndex; }
        public void setProposedIndex(String proposedIndex) { this.proposedIndex = proposedIndex; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }
    
    public static class UnusedIndex {
        private String schema;
        private String table;
        private String indexName;
        private long indexScans;
        private double sizeMB;
        private String suggestion;
        private String dropStatement;
        
        public UnusedIndex() {}
        
        public UnusedIndex(String schema, String table, String indexName, long indexScans, 
                          double sizeMB, String suggestion, String dropStatement) {
            this.schema = schema;
            this.table = table;
            this.indexName = indexName;
            this.indexScans = indexScans;
            this.sizeMB = sizeMB;
            this.suggestion = suggestion;
            this.dropStatement = dropStatement;
        }
        
        // Getters and setters
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public String getTable() { return table; }
        public void setTable(String table) { this.table = table; }
        public String getIndexName() { return indexName; }
        public void setIndexName(String indexName) { this.indexName = indexName; }
        public long getIndexScans() { return indexScans; }
        public void setIndexScans(long indexScans) { this.indexScans = indexScans; }
        public double getSizeMB() { return sizeMB; }
        public void setSizeMB(double sizeMB) { this.sizeMB = sizeMB; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public String getDropStatement() { return dropStatement; }
        public void setDropStatement(String dropStatement) { this.dropStatement = dropStatement; }
    }
    
    private List<MissingIndex> missingIndexes;
    private List<UnusedIndex> unusedIndexes;
    private int totalIndexesAnalyzed;
    private double totalWastedSpaceMB;
    
    public IndexRecommendationDTO() {}
    
    // Getters and setters
    public List<MissingIndex> getMissingIndexes() { return missingIndexes; }
    public void setMissingIndexes(List<MissingIndex> missingIndexes) { this.missingIndexes = missingIndexes; }
    public List<UnusedIndex> getUnusedIndexes() { return unusedIndexes; }
    public void setUnusedIndexes(List<UnusedIndex> unusedIndexes) { this.unusedIndexes = unusedIndexes; }
    public int getTotalIndexesAnalyzed() { return totalIndexesAnalyzed; }
    public void setTotalIndexesAnalyzed(int totalIndexesAnalyzed) { this.totalIndexesAnalyzed = totalIndexesAnalyzed; }
    public double getTotalWastedSpaceMB() { return totalWastedSpaceMB; }
    public void setTotalWastedSpaceMB(double totalWastedSpaceMB) { this.totalWastedSpaceMB = totalWastedSpaceMB; }
}
