package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DayWisePrediction {
    private LocalDate date;
    private String dayOfWeek;
    private Long predictedCount;
    private String trend;
    private Double confidenceScore;
}
