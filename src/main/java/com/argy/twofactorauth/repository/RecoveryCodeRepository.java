/**
 * Repositorio de códigos de recuperación.
 */
package com.argy.twofactorauth.repository;

import com.argy.twofactorauth.entity.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {
}