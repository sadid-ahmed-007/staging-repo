package com.eduauth.dto.access;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Read-only DTO representing an access request as seen by either
 * a verifier (their sent request) or a student (a request they received).
 *
 * Used in paginated list endpoints:
 *   GET /api/verifier/access-requests
 *   GET /api/student/access-requests
 */
@Data
public class AccessRequestListDto {

    private Long id;

    // Verifier details
    private String verifierName;        // contactPerson
    private String verifierCompany;     // companyName
    private String verifierEmail;

    // Student details (populated when the verifier is viewing)
    private Long studentId;
    private String studentName;

    private String purpose;
    private Integer requestedDurationDays;

    /**
     * Status: pending | approved | rejected | cancelled
     * ("cancelled" is returned for soft-deleted rows)
     */
    private String status;

    private LocalDateTime requestedAt;  // maps to createdAt
    private LocalDateTime respondedAt;
    private String responseMessage;

    /** Whether access is currently active (approved, not expired, not revoked) */
    private boolean hasActiveAccess;

    /** When the access expires, if approved */
    private LocalDateTime accessExpiresAt;

    // Certificate details (if requested for a specific certificate)
    private Long certificateId;
    private String certificateSerial;
    private String certificateName;
    private String certificateLevel;
    private String requestScope; // "specific_certificate" | "all_certificates"
}
