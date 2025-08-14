/**
 * Controlador REST para gestión de 2FA (Time-based One-Time Password, TOTP).
 * 
 * Endpoints:
 * - POST /api/2fa/enable: Genera un secreto TOTP, QR en data URI y códigos de recuperación para un usuario.
 * - POST /api/2fa/verify: Verifica un código TOTP para el usuario.
 */
package com.argy.twofactorauth.controller;

import com.argy.twofactorauth.dto.Enable2FARequest;
import com.argy.twofactorauth.dto.Enable2FAResponse;
import com.argy.twofactorauth.dto.Verify2FARequest;
import com.argy.twofactorauth.dto.Verify2FAResponse;
import com.argy.twofactorauth.service.TwoFactorAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorAuthController {

	private final TwoFactorAuthService twoFactorAuthService;

	public TwoFactorAuthController(TwoFactorAuthService twoFactorAuthService) {
		this.twoFactorAuthService = twoFactorAuthService;
	}

	/**
	 * Habilita 2FA para el usuario indicado.
	 *
	 * Request body: { "username": "<usuario>" }
	 * Response: { "qrCode": "data:image/png;base64,...", "recoveryCodes": ["xxxx-....", ...] }
	 */
	@PostMapping("/enable")
	public Enable2FAResponse enable2FA(@RequestBody Enable2FARequest request) throws Exception {
		return twoFactorAuthService.enable2FA(request);
	}

	/**
	 * Verifica un código TOTP para el usuario.
	 *
	 * Request body: { "username": "<usuario>", "code": "123456" }
	 * Response: { "verified": true|false }
	 */
	@PostMapping("/verify")
	public Verify2FAResponse verify2FA(@RequestBody Verify2FARequest request) throws Exception {
		return twoFactorAuthService.verify2FA(request);
	}
}   