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
public class ClusterOverviewDTO {
    private Integer totalNodes;
    private Integer healthyNodes;
    private Integer totalPods;
    private Map<String, Integer> podsByStatus;
    private Map<String, Integer> podsByNamespace;
    private String totalCpuCapacity;
    private String totalMemoryCapacity;
    private String usedCpu;
    private String usedMemory;
    private Double cpuUtilization;
    private Double memoryUtilization;
}
