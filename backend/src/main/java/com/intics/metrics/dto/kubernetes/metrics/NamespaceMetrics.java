package com.intics.metrics.dto.kubernetes.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceMetrics {
    private String namespace;
    private Integer podCount;
    private Long totalCpuUsageNanoCores;
    private Long totalMemoryUsageBytes;
    private String cpuUsagePercent;
    private String memoryUsagePercent;
}
