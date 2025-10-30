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
public class LogSearchResultDTO {
    private List<LogEntryDTO> logs;
    private Integer totalCount;
    private Map<String, Integer> logLevelCounts;
    private Map<String, Integer> namespaceCounts;
    private Map<String, Integer> podCounts;
    private String searchQuery;
    private String timeRange;
    private String namespace;
    private String searchTerm;
    private String logLevel;
    private Boolean demoMode;
}
