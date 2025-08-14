/**
 * Respuesta de verificación de 2FA.
 * - verified: indica si el código TOTP es válido en la ventana de tiempo actual
 */
package com.argy.twofactorauth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Verify2FAResponse {
    private boolean verified;
}
