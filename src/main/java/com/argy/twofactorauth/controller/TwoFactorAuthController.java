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
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import com.argy.twofactorauth.repository.UserRepository;
import com.argy.twofactorauth.entity.User;
import com.argy.twofactorauth.entity.RecoveryCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import io.github.bucket4j.Bucket;
import javax.servlet.http.HttpServletRequest;
import com.argy.twofactorauth.config.RateLimitConfig;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorAuthController {

	private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, Bucket> userBuckets;
    private final HttpServletRequest httpServletRequest;

	public TwoFactorAuthController(TwoFactorAuthService twoFactorAuthService, UserRepository userRepository, PasswordEncoder passwordEncoder, ConcurrentHashMap<String, Bucket> userBuckets, HttpServletRequest httpServletRequest) {
		this.twoFactorAuthService = twoFactorAuthService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userBuckets = userBuckets;
        this.httpServletRequest = httpServletRequest;
	}

	/**
	 * Habilita 2FA para el usuario indicado.
	 *
	 * Request body: { "username": "<usuario>" }
	 * Response: { "qrCode": "data:image/png;base64,...", "recoveryCodes": ["xxxx-....", ...] }
	 */
	@PostMapping("/enable")
	public Enable2FAResponse enable2FA(@Valid @RequestBody Enable2FARequest request) throws Exception {
		return twoFactorAuthService.enable2FA(request);
	}

	/**
	 * Verifica un código TOTP para el usuario.
	 *
	 * Request body: { "username": "<usuario>", "code": "123456" }
	 * Response: { "verified": true|false }
	 */
	@PostMapping("/verify")
	public Verify2FAResponse verify2FA(@Valid @RequestBody Verify2FARequest request) throws Exception {
		String key = request.getUsername() + "|" + httpServletRequest.getRemoteAddr();
		Bucket bucket = userBuckets.computeIfAbsent(key, k -> RateLimitConfig.newDefaultBucket());
		if (!bucket.tryConsume(1)) {
			throw new IllegalArgumentException("Rate limit excedido. Intenta nuevamente más tarde.");
		}
		return twoFactorAuthService.verify2FA(request);
	}

	/**
	 * Rota el secreto 2FA del usuario (genera uno nuevo, devuelve nuevo QR y recovery codes).
	 */
	@PostMapping("/rotate/{username}")
	public ResponseEntity<Enable2FAResponse> rotate(@PathVariable("username") String username) throws Exception {
		Enable2FARequest req = new Enable2FARequest();
		req.setUsername(username);
		Enable2FAResponse response = twoFactorAuthService.enable2FA(req);
		return ResponseEntity.ok(response);
	}

	/**
	 * Inhabilita 2FA para el usuario (elimina secreto y códigos de recuperación).
	 */
	@PostMapping("/disable/{username}")
	public ResponseEntity<Void> disable(@PathVariable("username") String username) {
		User user = userRepository.findByUsername(username);
		if (user != null) {
			user.setSecret(null);
			if (user.getRecoveryCodes() != null) {
				user.getRecoveryCodes().clear();
			}
			userRepository.save(user);
		}
		return ResponseEntity.noContent().build();
	}

	/**
	 * Verifica un código de recuperación (one-shot). Marca como usado si coincide.
	 */
	@PostMapping("/verify-recovery/{username}")
	public ResponseEntity<Verify2FAResponse> verifyRecovery(@PathVariable("username") String username, @RequestBody String recoveryCode) {
		String key = username + "|" + httpServletRequest.getRemoteAddr();
		Bucket bucket = userBuckets.computeIfAbsent(key, k -> RateLimitConfig.newDefaultBucket());
		if (!bucket.tryConsume(1)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new Verify2FAResponse(false));
		}
		User user = userRepository.findByUsername(username);
		if (user == null || user.getRecoveryCodes() == null || user.getRecoveryCodes().isEmpty()) {
			return ResponseEntity.ok(new Verify2FAResponse(false));
		}
		Optional<RecoveryCode> match = user.getRecoveryCodes().stream()
			.filter(rc -> !rc.isUsed() && passwordEncoder.matches(recoveryCode.replaceAll("\"", "").trim(), rc.getCodeHash()))
			.findFirst();
		if (match.isPresent()) {
			RecoveryCode rc = match.get();
			rc.setUsed(true);
			userRepository.save(user);
			return ResponseEntity.ok(new Verify2FAResponse(true));
		}
		return ResponseEntity.ok(new Verify2FAResponse(false));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}
}   