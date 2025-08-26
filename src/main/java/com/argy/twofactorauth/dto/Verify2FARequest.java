/**
 * Petición para verificar un código TOTP.
 * Campos:
 * - username: usuario a verificar
 * - code: código TOTP de 6 dígitos
 */
package com.argy.twofactorauth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class Verify2FARequest {
    @NotBlank
    @Size(min = 3, max = 128)
    private String username;
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String code;
}