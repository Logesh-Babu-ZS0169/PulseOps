package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionResponse {
    private LocalDate predictionDate;
    private Long predictedCount;
    private Long sevenDayAverage;
    private Long fourteenDayAverage;
    private Long thirtyDayAverage;
    private String trend;
    private Double confidenceScore;
    private List<HistoricalDataPoint> historicalData;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HistoricalDataPoint {
        private LocalDate date;
        private Long count;
        private String dayOfWeek;
    }
}
