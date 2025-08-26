/**
 * Petición para habilitar 2FA.
 * Campos:
 * - username: identificador del usuario para el que se habilita 2FA.
 */
package com.argy.twofactorauth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class Enable2FARequest {
    @NotBlank
    @Size(min = 3, max = 128)
    private String username;
}