package com.intics.metrics.dto.analytics;

import java.util.List;

public class ConfigOptimizationDTO {
    
    public static class ConfigSuggestion {
        private String parameter;
        private String currentValue;
        private String suggestedValue;
        private String reason;
        private String priority;
        private String impact;
        
        public ConfigSuggestion() {}
        
        public ConfigSuggestion(String parameter, String currentValue, String suggestedValue,
                               String reason, String priority, String impact) {
            this.parameter = parameter;
            this.currentValue = currentValue;
            this.suggestedValue = suggestedValue;
            this.reason = reason;
            this.priority = priority;
            this.impact = impact;
        }
        
        // Getters and setters
        public String getParameter() { return parameter; }
        public void setParameter(String parameter) { this.parameter = parameter; }
        public String getCurrentValue() { return currentValue; }
        public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }
        public String getSuggestedValue() { return suggestedValue; }
        public void setSuggestedValue(String suggestedValue) { this.suggestedValue = suggestedValue; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getImpact() { return impact; }
        public void setImpact(String impact) { this.impact = impact; }
    }
    
    private List<ConfigSuggestion> suggestions;
    private int totalMemoryMB;
    private int cpuCores;
    private int currentConnections;
    private int maxConnections;
    
    public ConfigOptimizationDTO() {}
    
    // Getters and setters
    public List<ConfigSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<ConfigSuggestion> suggestions) { this.suggestions = suggestions; }
    public int getTotalMemoryMB() { return totalMemoryMB; }
    public void setTotalMemoryMB(int totalMemoryMB) { this.totalMemoryMB = totalMemoryMB; }
    public int getCpuCores() { return cpuCores; }
    public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
    public int getCurrentConnections() { return currentConnections; }
    public void setCurrentConnections(int currentConnections) { this.currentConnections = currentConnections; }
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
}
