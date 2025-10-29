package com.intics.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private String fullName;

    public AuthResponse(String token, String username, String email, String fullName) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }
}
