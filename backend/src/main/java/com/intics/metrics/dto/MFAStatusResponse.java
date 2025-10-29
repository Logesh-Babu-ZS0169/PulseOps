package com.intics.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MFAStatusResponse {
    private boolean enabled;
    private LocalDateTime enabledAt;
    private LocalDateTime lastUsedAt;
    private Boolean backupCodesGenerated;
    private Long unusedRecoveryCodes;
    private Boolean enrollmentRequired;
}
