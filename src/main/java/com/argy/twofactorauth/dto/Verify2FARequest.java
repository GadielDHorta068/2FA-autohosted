package com.argy.twofactorauth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Verify2FARequest {
    private String username;
    private String code;
}