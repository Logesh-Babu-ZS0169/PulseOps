package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DayWisePredictionResponse {
    private List<DayWisePrediction> predictions;
    private String overallTrend;
    private Double averageConfidence;
    private String description;
}
