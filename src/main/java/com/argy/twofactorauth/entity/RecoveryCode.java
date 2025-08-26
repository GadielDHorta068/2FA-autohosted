/**
 * Entidad de código de recuperación asociado a un usuario.
 * - codeHash: hash del código (BCrypt)
 * - used: indicador de uso (no utilizado en endpoints actuales)
 */
package com.argy.twofactorauth.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class RecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;
    private boolean used;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}