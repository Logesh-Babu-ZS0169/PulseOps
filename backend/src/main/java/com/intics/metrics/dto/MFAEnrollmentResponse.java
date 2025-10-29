package com.intics.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MFAEnrollmentResponse {
    private String secretKey;
    private String qrCodeImage;
    private String qrCodeUrl;
    private String issuer;
    private String username;
    private String message;
}
