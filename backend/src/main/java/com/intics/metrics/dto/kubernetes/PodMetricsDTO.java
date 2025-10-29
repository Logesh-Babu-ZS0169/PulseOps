package com.intics.metrics.dto.kubernetes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodMetricsDTO {
    private String name;
    private String namespace;
    private String status;
    private String phase;
    private Integer restartCount;
    private String age;
    private LocalDateTime createdAt;
    private String node;
    
    private ResourceMetrics resources;
    private List<ContainerMetrics> containers;
    private Map<String, String> labels;
    
    private String healthStatus;
    private List<String> warnings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceMetrics {
        private String cpuUsage;
        private String memoryUsage;
        private Double cpuPercent;
        private Double memoryPercent;
        private String cpuRequest;
        private String cpuLimit;
        private String memoryRequest;
        private String memoryLimit;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerMetrics {
        private String name;
        private String image;
        private String status;
        private Integer restartCount;
        private String cpuUsage;
        private String memoryUsage;
    }
}
