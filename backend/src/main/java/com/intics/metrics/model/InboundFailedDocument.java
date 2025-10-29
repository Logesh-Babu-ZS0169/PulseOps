package com.intics.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InboundFailedDocument {
    private String downloadCompletedOn;
    private String transactionId;
    private String dcnId;
    private String siCaseId;
}
