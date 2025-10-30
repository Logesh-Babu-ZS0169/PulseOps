package com.intics.metrics.dto.kubernetes.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodMetrics {
    private String podName;
    private String namespace;
    private LocalDateTime timestamp;
    
    private Long cpuUsageNanoCores;
    private String cpuUsagePercent;
    private Long memoryUsageBytes;
    private String memoryUsagePercent;
    
    private Map<String, ContainerMetrics> containerMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerMetrics {
        private String name;
        private Long cpuUsageNanoCores;
        private Long memoryUsageBytes;
    }
}
