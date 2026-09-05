package com.eduauth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts / decrypts dates of birth for certificate share links.
 *
 * Uses AES-256-CBC with a key derived from the application's JWT secret.
 * Produces URL-safe Base64 tokens.
 */
@Service
public class EncryptionService {

    private final SecretKeySpec secretKey;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    public EncryptionService(@Value("${jwt.secret}") String jwtSecret) {
        try {
            // Derive a 256-bit key from the JWT secret via SHA-256
            // We use SHA-256 here to guarantee the output is exactly 32 bytes (256 bits),
            // which is the exact key length required by the AES-256 cipher, regardless of 
            // the length of the provided JWT secret string.
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptionService", e);
        }
    }

    /**
     * Encrypt a date of birth string (YYYY-MM-DD) into a URL-safe Base64 token.
     */
    public String encryptDOB(String dob) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            // Generate a random IV
            byte[] iv = new byte[IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encrypted = cipher.doFinal(dob.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            // URL-safe Base64
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("DOB encryption failed", e);
        }
    }

    /**
     * Decrypt a URL-safe Base64 token back to a DOB string (YYYY-MM-DD).
     *
     * @return the decrypted DOB string, or null if decryption fails or date is invalid.
     */
    public String decryptDOB(String token) {
        try {
            byte[] combined = Base64.getUrlDecoder().decode(token);
            if (combined.length < IV_LENGTH + 1) {
                return null;
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);

            String dob = new String(decrypted, StandardCharsets.UTF_8);

            // Validate that it's a real date
            LocalDate parsed = LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (!parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).equals(dob)) {
                return null;
            }

            return dob;
        } catch (Exception e) {
            return null;
        }
    }
}
