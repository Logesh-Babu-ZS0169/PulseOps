package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessFailedDocument {
    private String downloadCompletedOn;
    private String dcnId;
    private String siCaseId;
    private String status;
    private String errorCode;
    private String errorMessage;
    private String rootPipelineId;
    private String actionId;
    private String log;
}
