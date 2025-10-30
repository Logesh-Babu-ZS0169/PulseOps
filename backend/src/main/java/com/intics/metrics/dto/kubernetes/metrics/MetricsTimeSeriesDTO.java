package com.intics.metrics.dto.kubernetes.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsTimeSeriesDTO {
    private String resourceName;
    private String resourceType;
    private String metricType;
    private List<DataPoint> dataPoints;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private LocalDateTime timestamp;
        private Double value;
        private String unit;
    }
}
