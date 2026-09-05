package com.eduauth.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    @Column(name = "institution_id")
    private Long institutionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", insertable = false, updatable = false)
    private Enrollment enrollment;

    @Column(name = "issued_by")
    private Long issuedByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by", insertable = false, updatable = false)
    private User issuedBy;

    @Column(name = "serial", unique = true, nullable = false)
    private String serial;

    @Column(name = "certificate_level")
    private String certificateLevel;

    @Column(name = "certificate_name")
    private String certificateName;

    @Column(name = "department")
    private String department;

    @Column(name = "major")
    private String major;

    @Column(name = "session")
    private String session;

    @Column(name = "cgpa")
    private BigDecimal cgpa;

    @Column(name = "degree_class")
    private String degreeClass;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "convocation_date")
    private LocalDate convocationDate;

    @Column(name = "authority_name")
    private String authorityName;

    @Column(name = "authority_title")
    private String authorityTitle;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "issued_name")
    private String issuedName;

    @Column(name = "is_publicly_shareable")
    private Boolean isPubliclyShareable = true;

    // Revocation fields
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by", insertable = false, updatable = false)
    private User revokedBy;

    @Column(name = "revoked_by_role",
            columnDefinition = "ENUM('university','admin')")
    private String revokedByRole;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    // JSON column — stored/retrieved as raw String
    @Column(name = "revocation_history", columnDefinition = "JSON")
    private String revocationHistory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** Full name used on certificate — falls back to user name if issuedName is blank. */
    public String getStudentDisplayName() {
        if (student == null) return "N/A";
        if (issuedName != null && !issuedName.isBlank()) return issuedName;
        if (student.getFirstName() != null) {
            return (student.getFirstName() + " "
                    + (student.getMiddleName() != null ? student.getMiddleName() + " " : "")
                    + student.getLastName()).trim();
        }
        return student.getUser() != null ? student.getUser().getEmail() : "N/A";
    }
}
