package com.intics.metrics.dto.kubernetes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeMetricsDTO {
    private String name;
    private String status;
    private Map<String, String> capacity;
    private Map<String, String> allocatable;
    private String cpuUsage;
    private String memoryUsage;
    private Double cpuPercent;
    private Double memoryPercent;
    private Integer podCount;
    private Integer maxPods;
    private String version;
    private String osImage;
    private Map<String, String> labels;
}
