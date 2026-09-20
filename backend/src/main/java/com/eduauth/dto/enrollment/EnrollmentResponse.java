package com.eduauth.dto.enrollment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EnrollmentResponse {
    private Long id;
    private String enrollmentNumber;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentIdInUniversity;   // maps to rollNumber
    private String program;
    private String department;
    private String major;
    private Long certificateLevelId;
    private Long departmentId;
    private Long majorId;
    private Long programId;
    private String programName;
    private String programShortName;
    private String batch;

    /**
     * Status in API response may be "withdrawal_requested" even though
     * DB stores "active" with a pending WithdrawalRequest — we compute
     * this at response-build time.
     */
    private String status;

    private LocalDate enrollmentDate;
    private LocalDate expectedGraduationDate;
    private LocalDate actualGraduationDate;
    private String institutionName;
    private LocalDateTime createdAt;

    // Withdrawal info (populated when status = withdrawal_requested)
    private String withdrawalReason;
    private LocalDateTime withdrawalRequestedAt;

    /**
     * Full withdrawal details for university view.
     * Populated when a withdrawal request exists OR when status = withdrawn (direct or approved).
     */
    private WithdrawalRequestInfo withdrawalRequest;

    // Certificate info (populated when status = graduated)
    private Long certificateId;
    private String certificateSerial;
}
