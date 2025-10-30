package com.intics.metrics.dto.kubernetes.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogStatisticsDTO {
    private Integer totalLogs;
    private Map<String, Integer> logsByLevel;
    private Map<String, Integer> logsByNamespace;
    private Map<String, Integer> logsByPod;
    private Map<String, Integer> errorsByPod;
    private Integer errorCount;
    private Integer warningCount;
    private Integer infoCount;
    private String timeRange;
    private String namespace;
    private Integer timeRangeSeconds;
    private Map<String, Integer> logLevelCounts;
    private List<PodLogStatDTO> podStatistics;
    private List<PodLogStatDTO> topErrorPods;
    private Boolean demoMode;
}
