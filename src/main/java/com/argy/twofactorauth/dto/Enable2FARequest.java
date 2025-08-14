package com.argy.twofactorauth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Enable2FARequest {
    private String username;
}