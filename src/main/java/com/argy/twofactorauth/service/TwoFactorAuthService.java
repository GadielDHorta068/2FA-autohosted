/**
 * Servicio de negocio para habilitar y verificar 2FA (TOTP).
 *
 * Responsabilidades:
 * - Generar secreto TOTP y QR (data URI) usando la librería dev.samstevens.totp
 * - Generar y almacenar códigos de recuperación asociados al usuario
 * - Verificar códigos TOTP recibidos contra el secreto cifrado
 */
package com.argy.twofactorauth.service;

import com.argy.twofactorauth.dto.Enable2FARequest;
import com.argy.twofactorauth.dto.Enable2FAResponse;
import com.argy.twofactorauth.dto.Verify2FARequest;
import com.argy.twofactorauth.dto.Verify2FAResponse;
import com.argy.twofactorauth.entity.RecoveryCode;
import com.argy.twofactorauth.entity.User;
import com.argy.twofactorauth.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.stereotype.Service;
import dev.samstevens.totp.util.Utils;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class TwoFactorAuthService {

	private final UserRepository userRepository;
	private final SecretGenerator secretGenerator;
	private final QrDataFactory qrDataFactory;
	private final QrGenerator qrGenerator;
	private final CodeVerifier codeVerifier;
	private final RecoveryCodeGenerator recoveryCodeGenerator;
	private final EncryptionService encryptionService;
	private final PasswordEncoder passwordEncoder;

	public TwoFactorAuthService(UserRepository userRepository, SecretGenerator secretGenerator, QrDataFactory qrDataFactory, QrGenerator qrGenerator, CodeVerifier codeVerifier, RecoveryCodeGenerator recoveryCodeGenerator, EncryptionService encryptionService, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.secretGenerator = secretGenerator;
		this.qrDataFactory = qrDataFactory;
		this.qrGenerator = qrGenerator;
		this.codeVerifier = codeVerifier;
		this.recoveryCodeGenerator = recoveryCodeGenerator;
		this.encryptionService = encryptionService;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Habilita 2FA para el usuario (crea secreto, QR y códigos de recuperación, persiste en DB).
	 * @param request contiene el username
	 * @return data URI del QR y lista de códigos de recuperación en claro (solo en la respuesta)
	 * @throws Exception en caso de errores de cifrado o generación de QR
	 */
	@Transactional
	public Enable2FAResponse enable2FA(Enable2FARequest request) throws Exception {
		String secret = secretGenerator.generate();
		User foundUser = userRepository.findByUsername(request.getUsername());
		User user = (foundUser != null) ? foundUser : new User();
		if (foundUser == null) {
			user.setUsername(request.getUsername());
		}
		user.setSecret(encryptionService.encrypt(secret));
		final User userRef = user;

		QrData qrData = qrDataFactory.newBuilder()
			.label("Mi App (" + userRef.getUsername() + ")")
			.secret(secret)
			.issuer("Mi App")
			.build();
		String mimeType = qrGenerator.getImageMimeType();
		String qrCode = Utils.getDataUriForImage(qrGenerator.generate(qrData), mimeType);

		String[] recoveryCodes = recoveryCodeGenerator.generateCodes(10);
		user.setRecoveryCodes(Arrays.stream(recoveryCodes)
			.map(code -> {
				RecoveryCode rc = new RecoveryCode();
				rc.setCodeHash(passwordEncoder.encode(code));
				rc.setUser(userRef);
				return rc;
			}).collect(Collectors.toList()));
		userRepository.save(userRef);

		return new Enable2FAResponse(qrCode, Arrays.asList(recoveryCodes));
	}

	/**
	 * Verifica un código TOTP para el username dado.
	 * @param request contiene username y code (6 dígitos)
	 * @return resultado booleano de verificación
	 * @throws Exception si falla el descifrado del secreto
	 */
	public Verify2FAResponse verify2FA(Verify2FARequest request) throws Exception {
		User user = userRepository.findByUsername(request.getUsername());
		if (user == null || user.getSecret() == null) {
			return new Verify2FAResponse(false);
		}

		String decryptedSecret = encryptionService.decrypt(user.getSecret());
		boolean verified = codeVerifier.isValidCode(decryptedSecret, request.getCode());
		return new Verify2FAResponse(verified);
	}
}
