package com.eduauth.dto.application;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for a university application.
 *
 * <p>{@code studentCertificates} is only populated when the university
 * views a specific application — it is null/empty in student-facing views.</p>
 */
@Data
public class ApplicationResponse {

    private Long id;

    // ── Student info ────────────────────────────────────────────────────────
    private Long studentId;
    private String studentName;
    private String studentEmail;

    // ── University info ─────────────────────────────────────────────────────
    private Long universityId;
    private String universityName;

    // ── Program structure ────────────────────────────────────────────────────
    private Long certificateLevelId;
    private String certificateLevelName;

    private Long departmentId;
    private String departmentName;

    private Long programId;
    private String programName;

    // ── Application content ──────────────────────────────────────────────────
    private String personalStatement;

    // ── Status & timeline ────────────────────────────────────────────────────
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime cancelledAt;

    // ── Decision fields ──────────────────────────────────────────────────────
    private String rejectionReason;
    private String acceptanceMessage;

    // ── University review only — student certificates ────────────────────────
    /**
     * Populated only when a university fetches a specific application.
     * Contains the student's certificate list for evaluation purposes.
     * Each map contains: serial, certificateName, certificateLevel, institution, issueDate, status.
     */
    private List<Map<String, Object>> studentCertificates;
}
