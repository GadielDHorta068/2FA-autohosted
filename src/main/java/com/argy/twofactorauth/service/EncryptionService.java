package com.argy.twofactorauth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${encryption.key}")
    private String encryptionKey;

    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12; // recomendado por NIST

    private SecretKey buildSecretKey() throws Exception {
        // Deriva una clave AES-256 a partir de la passphrase usando SHA-256.
        // Esto evita requisitos de longitud exacta (16/24/32) y estandariza a 32 bytes.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = buildSecretKey();
        byte[] iv = generateIv();
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        byte[] ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        // Formato: base64(iv):base64(ciphertext)
        String ivB64 = Base64.getEncoder().encodeToString(iv);
        String ctB64 = Base64.getEncoder().encodeToString(ciphertext);
        return ivB64 + ":" + ctB64;
    }

    public String decrypt(String encryptedData) throws Exception {
        String[] parts = encryptedData.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid encrypted data format");
        }
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] ct = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = buildSecretKey();
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        byte[] plaintext = cipher.doFinal(ct);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}