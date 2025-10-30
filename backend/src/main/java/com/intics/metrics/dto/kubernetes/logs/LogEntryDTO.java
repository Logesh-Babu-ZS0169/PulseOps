package com.intics.metrics.dto.kubernetes.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryDTO {
    private String timestamp;
    private String podName;
    private String namespace;
    private String containerName;
    private String logLevel;
    private String message;
    private String node;
    private Integer lineNumber;
}
