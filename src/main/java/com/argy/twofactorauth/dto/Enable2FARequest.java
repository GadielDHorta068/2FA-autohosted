/**
 * Petición para habilitar 2FA.
 * Campos:
 * - username: identificador del usuario para el que se habilita 2FA.
 */
package com.argy.twofactorauth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Enable2FARequest {
    private String username;
}