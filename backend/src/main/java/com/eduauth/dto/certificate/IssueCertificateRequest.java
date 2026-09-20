package com.eduauth.dto.certificate;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for issuing a single certificate.
 */
@Data
public class IssueCertificateRequest {

    /** The student's ID from the students table (not the user ID). */
    @NotNull(message = "studentId is required")
    @JsonAlias({"student_id", "studentId"})
    private Long studentId;

    /** The optional enrollment ID — if omitted, the active/graduated enrollment is resolved automatically. */
    @JsonAlias({"enrollment_id", "enrollmentId"})
    private Long enrollmentId;

    /**
     * Optional program ID from the academic program structure.
     * When provided, certificateName, department, and certificateLevel are auto-resolved
     * from the program hierarchy (Program → Department → CertificateLevel).
     * Explicit values in this request will override the auto-resolved values.
     */
    @JsonAlias({"program_id", "programId"})
    private Long programId;

    /** Human-readable certificate name, e.g. "Bachelor of Science in Computer Science" */
    @NotBlank(message = "certificateName is required")
    @JsonAlias({"certificate_name", "certificateName"})
    private String certificateName;

    /** The degree level used for serial prefix mapping, e.g. "Bachelor of Science", "MBA", "PhD" */
    @NotBlank(message = "certificateLevel is required")
    @JsonAlias({"certificate_level", "certificateLevel"})
    private String certificateLevel;

    /** Department name, e.g. "Computer Science and Engineering" */
    @JsonAlias("department")
    private String department;

    /** Major / specialization, e.g. "Software Engineering" */
    @JsonAlias("major")
    private String major;

    /** Academic session, e.g. "Spring 2026" */
    @NotBlank(message = "session is required")
    @JsonAlias("session")
    private String session;

    /** CGPA on a 0.00–4.00 scale (optional) */
    @DecimalMin(value = "0.00", message = "cgpa must be ≥ 0.00")
    @DecimalMax(value = "4.00", message = "cgpa must be ≤ 4.00")
    @JsonAlias("cgpa")
    private BigDecimal cgpa;

    /** Degree classification, e.g. "First Class", "Second Class" (optional) */
    @JsonAlias({"degree_class", "degreeClass"})
    private String degreeClass;

    /** Date the certificate is issued (required) */
    @NotNull(message = "issueDate is required")
    @JsonAlias({"issue_date", "issueDate"})
    private LocalDate issueDate;

    /** Convocation / ceremony date (optional) */
    @JsonAlias({"convocation_date", "convocationDate"})
    private LocalDate convocationDate;

    /** Name of the signing authority, e.g. "Prof. Dr. John Smith" */
    @NotBlank(message = "authorityName is required")
    @JsonAlias({"authority_name", "authorityName"})
    private String authorityName;

    /** Title of the signing authority, e.g. "Vice Chancellor" */
    @NotBlank(message = "authorityTitle is required")
    @JsonAlias({"authority_title", "authorityTitle"})
    private String authorityTitle;

    /** Override the student's legal name on the certificate (optional) */
    @JsonAlias({"issued_name", "issuedName"})
    private String issuedName;
}
