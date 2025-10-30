package com.intics.metrics.dto.kubernetes.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodLogStatDTO {
    private String podName;
    private String namespace;
    private Integer totalLogs;
    private Integer errorCount;
    private Integer warningCount;
    private Integer infoCount;
    private Integer debugCount;
}
