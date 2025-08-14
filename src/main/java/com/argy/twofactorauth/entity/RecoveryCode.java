/**
 * Entidad de código de recuperación asociado a un usuario.
 * - code: valor cifrado
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
    private String code;
    private boolean used;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}