package com.intics.metrics.dto.kubernetes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamespaceDTO {
    private String name;
    private String status;
    private Integer podCount;
    private LocalDateTime createdAt;
}
