package com.intics.metrics.dto.kubernetes.metrics;

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
public class ClusterMetricsDTO {
    private LocalDateTime timestamp;
    
    private Long totalCpuUsageNanoCores;
    private Long totalCpuCapacityNanoCores;
    private String cpuUsagePercent;
    
    private Long totalMemoryUsageBytes;
    private Long totalMemoryCapacityBytes;
    private String memoryUsagePercent;
    
    private Long totalDiskUsageBytes;
    private Long totalDiskCapacityBytes;
    private String diskUsagePercent;
    
    private Integer totalPods;
    private Integer runningPods;
    private Integer pendingPods;
    private Integer failedPods;
    
    private List<NodeMetrics> nodeMetrics;
    private List<PodMetrics> topPodsByCpu;
    private List<PodMetrics> topPodsByMemory;
    
    private Map<String, NamespaceMetrics> metricsByNamespace;
}
