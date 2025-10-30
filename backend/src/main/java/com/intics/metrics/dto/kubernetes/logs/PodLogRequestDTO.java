package com.intics.metrics.dto.kubernetes.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodLogRequestDTO {
    private String namespace;
    private String podName;
    private String containerName;
    private Integer tailLines;
    private Boolean follow;
    private Integer sinceSeconds;
    private Boolean timestamps;
    private String logLevel;
    private String searchTerm;
}
