/**
 * Petición para verificar un código TOTP.
 * Campos:
 * - username: usuario a verificar
 * - code: código TOTP de 6 dígitos
 */
package com.argy.twofactorauth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Verify2FARequest {
    private String username;
    private String code;
}