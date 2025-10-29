package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HourlyPrediction {
    private LocalDateTime hourStart;
    private LocalDateTime hourEnd;
    private Long predictedCount;
    private Double confidenceScore;
    private Integer hourOfDay;
    private String timeLabel;
}
