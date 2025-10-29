package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecoveredDocument {
    private String dcnId;
    private String siCaseId;
    private String documentType;
    private String downloadCompletedOn;
}
