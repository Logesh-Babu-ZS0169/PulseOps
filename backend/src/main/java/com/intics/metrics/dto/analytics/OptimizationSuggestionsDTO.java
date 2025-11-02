package com.intics.metrics.dto.analytics;

import java.util.List;

public class OptimizationSuggestionsDTO {
    private List<Suggestion> suggestions;
    private int totalSuggestions;
    private int highPriority;
    private int mediumPriority;
    private int lowPriority;

    public static class Suggestion {
        private String category;
        private String priority;
        private String title;
        private String description;
        private String targetObject;
        private double potentialSavingsMB;
        private List<String> actionSteps;

        public Suggestion() {}

        public Suggestion(String category, String priority, String title, String description,
                        String targetObject, double potentialSavingsMB, List<String> actionSteps) {
            this.category = category;
            this.priority = priority;
            this.title = title;
            this.description = description;
            this.targetObject = targetObject;
            this.potentialSavingsMB = potentialSavingsMB;
            this.actionSteps = actionSteps;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTargetObject() { return targetObject; }
        public void setTargetObject(String targetObject) { this.targetObject = targetObject; }
        public double getPotentialSavingsMB() { return potentialSavingsMB; }
        public void setPotentialSavingsMB(double potentialSavingsMB) { this.potentialSavingsMB = potentialSavingsMB; }
        public List<String> getActionSteps() { return actionSteps; }
        public void setActionSteps(List<String> actionSteps) { this.actionSteps = actionSteps; }
    }

    public OptimizationSuggestionsDTO() {}

    public List<Suggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Suggestion> suggestions) { this.suggestions = suggestions; }
    public int getTotalSuggestions() { return totalSuggestions; }
    public void setTotalSuggestions(int totalSuggestions) { this.totalSuggestions = totalSuggestions; }
    public int getHighPriority() { return highPriority; }
    public void setHighPriority(int highPriority) { this.highPriority = highPriority; }
    public int getMediumPriority() { return mediumPriority; }
    public void setMediumPriority(int mediumPriority) { this.mediumPriority = mediumPriority; }
    public int getLowPriority() { return lowPriority; }
    public void setLowPriority(int lowPriority) { this.lowPriority = lowPriority; }
}
