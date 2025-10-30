package com.intics.metrics.dto.kubernetes.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeMetrics {
    private String nodeName;
    private LocalDateTime timestamp;
    
    private Long cpuUsageNanoCores;
    private String cpuUsagePercent;
    private Long cpuCapacityNanoCores;
    
    private Long memoryUsageBytes;
    private String memoryUsagePercent;
    private Long memoryCapacityBytes;
    
    private Long diskUsageBytes;
    private String diskUsagePercent;
    private Long diskCapacityBytes;
    
    private Long networkReceiveBytes;
    private Long networkTransmitBytes;
}
