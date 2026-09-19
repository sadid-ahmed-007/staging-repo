package com.eduauth.dto.access;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Read-only DTO representing an active (or historical) access grant.
 *
 * Used in:
 *   GET /api/student/access-grants   (student viewing who has access)
 *   GET /api/verifier/accessible-certificates  (verifier viewing their grants)
 */
@Data
public class AccessGrantListDto {

    private Long id;

    // Verifier details
    private Long verifierId;
    private String verifierName;
    private String verifierCompany;
    private String verifierEmail;

    // Student details (when shown to verifier side)
    private Long studentId;
    private String studentName;
    private String studentEmail;

    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    /** true if revokedAt is null AND expiresAt is in the future */
    private boolean isActive;

    /** Computed: max(0, ChronoUnit.DAYS between now and expiresAt). 0 if expired/revoked. */
    private long daysRemaining;

    // Certificate details (if granted for a specific certificate)
    private Long certificateId;
    private String certificateSerial;
    private String certificateName;
    private String certificateLevel;
    private String grantScope; // "specific_certificate" | "all_certificates"
}
