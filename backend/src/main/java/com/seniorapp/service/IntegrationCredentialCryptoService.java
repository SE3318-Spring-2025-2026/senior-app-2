package com.seniorapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class IntegrationCredentialCryptoService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String ENCRYPTED_PREFIX = "enc:v1:";

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public IntegrationCredentialCryptoService(
            @Value("${app.integrations.encryption-key:}") String encryptionKey,
            @Value("${app.jwt.secret}") String jwtSecret) {
        String keyMaterial = (encryptionKey != null && !encryptionKey.isBlank()) ? encryptionKey : jwtSecret;
        this.secretKey = buildKey(keyMaterial);
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String cipherBase64 = Base64.getEncoder().encodeToString(cipherText);
            return ENCRYPTED_PREFIX + ivBase64 + ":" + cipherBase64;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encrypt integration credentials.", ex);
        }
    }

    public String encryptIfNeeded(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "";
        }
        if (isEncrypted(candidate)) {
            return candidate;
        }
        return encrypt(candidate.trim());
    }

    public boolean isEncrypted(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.startsWith(ENCRYPTED_PREFIX)) {
            return true;
        }
        return canDecryptLegacyPayload(value);
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return "";
        }
        try {
            if (encrypted.startsWith(ENCRYPTED_PREFIX)) {
                String[] parts = encrypted.split(":", 4);
                if (parts.length != 4) {
                    throw new IllegalArgumentException("Invalid encrypted payload format.");
                }
                byte[] iv = Base64.getDecoder().decode(parts[2]);
                byte[] cipherText = Base64.getDecoder().decode(parts[3]);
                return decryptPayload(iv, cipherText);
            }
            return decryptLegacyPayload(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decrypt integration credentials.", ex);
        }
    }

    private SecretKeySpec buildKey(String keyMaterial) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not initialize encryption key.", ex);
        }
    }

    private String decryptLegacyPayload(String encrypted) throws Exception {
        byte[] payload = Base64.getDecoder().decode(encrypted);
        if (payload.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid encrypted payload.");
        }
        byte[] iv = new byte[IV_LENGTH_BYTES];
        byte[] cipherText = new byte[payload.length - IV_LENGTH_BYTES];
        System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
        System.arraycopy(payload, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);
        return decryptPayload(iv, cipherText);
    }

    private String decryptPayload(byte[] iv, byte[] cipherText) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] plainBytes = cipher.doFinal(cipherText);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    private boolean canDecryptLegacyPayload(String value) {
        try {
            decryptLegacyPayload(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
