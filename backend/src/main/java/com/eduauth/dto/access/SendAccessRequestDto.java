package com.eduauth.dto.access;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body sent by a verifier to create a new access request.
 *
 * POST /api/verifier/access-requests
 */
@Data
public class SendAccessRequestDto {

    /** student.id (primary key of the students table — NOT the university-assigned student_id string). */
    @NotNull(message = "Student ID is required")
    private Long studentId;

    /**
     * Optional certificate ID when requesting access to a specific certificate.
     * Null when requesting access to all certificates.
     */
    private Long certificateId;

    /**
     * Optional explicit flag: true to request all certificates.
     */
    private Boolean requestAllCertificates;

    @NotBlank(message = "Purpose is required")
    @Size(min = 20, max = 500, message = "Purpose must be between 20 and 500 characters")
    private String purpose;

    @NotNull(message = "Requested duration is required")
    @Min(value = 1,   message = "Duration must be at least 1 day")
    @Max(value = 365, message = "Duration cannot exceed 365 days")
    private Integer requestedDurationDays;
}
