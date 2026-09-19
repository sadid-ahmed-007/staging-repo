package com.eduauth.dto.certificate;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO returned after a batch certificate issuance operation.
 */
@Data
public class BatchIssueResult {

    /** Total number of data rows processed (excluding the header row). */
    private int totalProcessed;

    /** Number of certificates successfully issued. */
    private int successful;

    /** Number of rows that failed. */
    private int failed;

    /** Details of each failed row. */
    private List<RowError> errors = new ArrayList<>();

    /** Details of each successfully issued certificate. */
    private List<IssuedSerial> issuedSerials = new ArrayList<>();

    // ── Inner record types ────────────────────────────────────────────────────

    /** Represents a single row that failed processing. */
    @Data
    public static class RowError {
        private int    rowNumber;
        private String studentEmail;
        private String reason;

        public RowError(int rowNumber, String studentEmail, String reason) {
            this.rowNumber    = rowNumber;
            this.studentEmail = studentEmail;
            this.reason       = reason;
        }
    }

    /** Represents a certificate that was successfully issued. */
    @Data
    public static class IssuedSerial {
        private String studentEmail;
        private String serial;
        private String studentName;

        public IssuedSerial(String studentEmail, String serial, String studentName) {
            this.studentEmail = studentEmail;
            this.serial       = serial;
            this.studentName  = studentName;
        }
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public void addError(int rowNumber, String studentEmail, String reason) {
        this.failed++;
        this.totalProcessed++;
        this.errors.add(new RowError(rowNumber, studentEmail, reason));
    }

    public void addSuccess(String studentEmail, String serial, String studentName) {
        this.successful++;
        this.totalProcessed++;
        this.issuedSerials.add(new IssuedSerial(studentEmail, serial, studentName));
    }
}
