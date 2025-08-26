/**
 * Entidad de usuario con credenciales 2FA:
 * - username: identificador único
 * - secret: secreto TOTP cifrado (AES)
 * - recoveryCodes: códigos de recuperación cifrados
 */
package com.argy.twofactorauth.entity;

import javax.persistence.*;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 128)
    @NotBlank
    @Size(min = 3, max = 128)
    private String username;
    private String secret;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<RecoveryCode> recoveryCodes;
}