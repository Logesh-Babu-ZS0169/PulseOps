package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HourlyPredictionResponse {
    private LocalDate predictionDate;
    private Long totalPredictedCount;
    private List<HourlyPrediction> hourlyPredictions;
    private Double overallConfidence;
    private String description;
}
