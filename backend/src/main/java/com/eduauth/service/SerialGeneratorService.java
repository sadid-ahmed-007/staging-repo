package com.eduauth.service;

import org.springframework.stereotype.Service;

@Service
public class SerialGeneratorService {

    /**
     * Calculate checksum using modulo 32 algorithm.
     * Returns single character from alphanumeric set (excluding similar-looking chars).
     */
    private static String calculateChecksum(String prefix, String year, String sequence) {
        // Checksum character set (excluding 0, O, I, 1 to avoid confusion)
        String charset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        String data = prefix + year + sequence;

        int sum = 0;
        for (int i = 0; i < data.length(); i++) {
            sum += data.charAt(i);
        }

        int index = sum % charset.length();

        return String.valueOf(charset.charAt(index));
    }

    /**
     * Validate serial checksum.
     */
    public static boolean validateChecksum(String serial) {
        if (serial == null) {
            return false;
        }

        // Parse serial: BSc-26-000001M
        String[] parts = serial.split("-");

        if (parts.length != 3) {
            return false;
        }

        String prefix = parts[0];
        String year = parts[1];
        String seqWithChecksum = parts[2];

        if (seqWithChecksum.length() != 7) { // 6 digits + 1 checksum
            return false;
        }

        String sequence = seqWithChecksum.substring(0, 6);
        String providedChecksum = seqWithChecksum.substring(6, 7);

        String expectedChecksum = calculateChecksum(prefix, year, sequence);

        return providedChecksum.equals(expectedChecksum);
    }
}
