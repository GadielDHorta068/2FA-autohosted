/**
 * Respuesta de habilitación de 2FA.
 * Incluye:
 * - qrCode: data URI de imagen PNG para escanear en la app TOTP
 * - recoveryCodes: lista de códigos de recuperación en claro (mostrar una sola vez)
 */
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