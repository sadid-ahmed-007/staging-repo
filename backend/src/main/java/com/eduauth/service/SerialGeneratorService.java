package com.eduauth.service;

import com.eduauth.model.CertificateSequence;
import com.eduauth.repository.CertificateSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Generates and validates certificate serial numbers.
 *
 * Serial format: PREFIX-YY-NNNNNNC
 *   e.g. BSC-26-000001M, MBA-26-000042K, PHD-26-000003R
 *
 * Checksum algorithm:
 *   charset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
 *   data    = prefix + year + sequenceString (no dashes, e.g. "BSC26000001")
 *   sum     = sum of ASCII values of every character in data
 *   index   = sum % charset.length()
 *   checksum = charset.charAt(index)
 *
 * Degree prefix mapping (case-insensitive, matched via containment / startsWith):
 *   Bachelor of Science / BSc          → BSC
 *   Bachelor of Arts / BA              → BA
 *   Bachelor of Commerce / BCom        → BCOM
 *   Master of Science / MSc            → MSC
 *   Master of Business Administration  → MBA
 *   Doctor of Philosophy / PhD         → PHD
 *   Diploma                            → DIP
 *   Any other                          → CRT
 */
@Service
@RequiredArgsConstructor
public class SerialGeneratorService {

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final CertificateSequenceRepository sequenceRepository;

    // ── Prefix mapping ───────────────────────────────────────────────────────

    /**
     * Derive the serial prefix from the certificateLevel string.
     * Matching is case-insensitive.
     */
    public static String resolvePrefix(String certificateLevel) {
        if (certificateLevel == null || certificateLevel.isBlank()) {
            return "CRT";
        }
        String lc = certificateLevel.trim().toLowerCase();

        if (lc.contains("master of business") || lc.equals("mba")) {
            return "MBA";
        }
        if (lc.contains("master of science") || lc.equals("msc") || lc.startsWith("m.sc")) {
            return "MSC";
        }
        if (lc.contains("master")) {
            return "MSC";
        }
        if (lc.contains("bachelor of science") || lc.equals("bsc") || lc.startsWith("b.sc")) {
            return "BSC";
        }
        if (lc.contains("bachelor of arts") || lc.equals("ba") || lc.equals("b.a")) {
            return "BA";
        }
        if (lc.contains("bachelor of commerce") || lc.equals("bcom") || lc.equals("b.com")) {
            return "BCOM";
        }
        if (lc.contains("bachelor")) {
            return "BSC";
        }
        if (lc.contains("doctor of philosophy") || lc.equals("phd") || lc.equals("ph.d")) {
            return "PHD";
        }
        if (lc.contains("doctor")) {
            return "PHD";
        }
        if (lc.contains("diploma") || lc.equals("dip")) {
            return "DIP";
        }
        return "CRT";
    }

    // ── Serial generation ────────────────────────────────────────────────────

    /**
     * Generate a new unique serial for the given certificate level.
     *
     * Uses SERIALIZABLE isolation + pessimistic write lock on certificate_sequences
     * to prevent duplicate serials under concurrent load.
     *
     * @param certificateLevel e.g. "Bachelor of Science", "MBA", "PhD"
     * @return serial string e.g. "BSC-26-000001M"
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String generate(String certificateLevel) {
        String prefix = resolvePrefix(certificateLevel);
        String year   = String.valueOf(LocalDate.now().getYear()).substring(2); // 2-digit year

        // SELECT … FOR UPDATE — prevents concurrent generation of same sequence number
        CertificateSequence seq = sequenceRepository
                .findByPrefixAndYearSuffixWithLock(prefix, year)
                .orElseGet(() -> {
                    CertificateSequence newSeq = new CertificateSequence();
                    newSeq.setPrefix(prefix);
                    newSeq.setYearSuffix(year);
                    newSeq.setSequenceKey(prefix + year);
                    newSeq.setCurrentSequence(0L);
                    return newSeq;
                });

        // Increment sequence and persist before releasing lock
        long nextSeq = seq.getCurrentSequence() + 1;
        seq.setCurrentSequence(nextSeq);
        seq.setLastGeneratedAt(LocalDateTime.now());
        sequenceRepository.saveAndFlush(seq); // flush immediately inside the lock

        // Build the 6-digit zero-padded sequence string
        String sequenceStr = String.format("%06d", nextSeq);

        // Calculate checksum
        String checksum = calculateChecksum(prefix, year, sequenceStr);

        // Format: PREFIX-YY-NNNNNNC
        return prefix + "-" + year + "-" + sequenceStr + checksum;
    }

    // ── Checksum ─────────────────────────────────────────────────────────────

    /**
     * Calculate checksum character from prefix, year, and 6-digit sequence string.
     * data = prefix + year + sequenceString (no dashes, e.g. "BSC26000001")
     */
    private static String calculateChecksum(String prefix, String year, String sequence) {
        String data = prefix + year + sequence;

        int sum = 0;
        for (int i = 0; i < data.length(); i++) {
            sum += data.charAt(i);
        }

        int index = sum % CHARSET.length();
        return String.valueOf(CHARSET.charAt(index));
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Validate the checksum of a serial number.
     *
     * @param serial e.g. "BSC-26-000001M"
     * @return true if serial is valid and checksum matches, false otherwise
     */
    public static boolean validateChecksum(String serial) {
        if (serial == null) {
            return false;
        }

        // Parse serial: PREFIX-YY-NNNNNNC
        String[] parts = serial.split("-");

        if (parts.length != 3) {
            return false;
        }

        String prefix          = parts[0];
        String year            = parts[1];
        String seqWithChecksum = parts[2];

        // Must be exactly 7 chars: 6 digits + 1 checksum char
        if (seqWithChecksum.length() != 7) {
            return false;
        }

        String sequence        = seqWithChecksum.substring(0, 6);
        String providedChecksum = seqWithChecksum.substring(6, 7);

        String expectedChecksum = calculateChecksum(prefix, year, sequence);

        return providedChecksum.equals(expectedChecksum);
    }
}
