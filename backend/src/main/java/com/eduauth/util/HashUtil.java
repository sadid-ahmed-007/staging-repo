package com.eduauth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic utility methods.
 *
 * SHA-256 is used exclusively for hashing NID numbers before persistence
 * so that the raw NID value is never stored in the database.
 */
public final class HashUtil {

    private HashUtil() {
        // Utility class — no instantiation
    }

    /**
     * Computes the SHA-256 hash of the given input string.
     *
     * @param input the plain-text value to hash (e.g., a raw NID number)
     * @return lowercase hexadecimal SHA-256 digest of the input
     * @throws RuntimeException if SHA-256 is not available (should never happen on JVM)
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to lowercase hex string
            StringBuilder hexBuilder = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hexBuilder.append(String.format("%02x", b));
            }
            return hexBuilder.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java spec — this branch is unreachable
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
