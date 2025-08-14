package com.argy.twofactorauth.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enable2FAResponse {
    private String qrCode;
    private List<String> recoveryCodes;
}