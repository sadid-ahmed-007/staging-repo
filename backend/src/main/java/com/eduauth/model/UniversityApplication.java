package com.eduauth.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a student application to a university (institution).
 *
 * <p>The column {@code university_id} references {@code institutions.id} —
 * the codebase uses the {@link Institution} entity everywhere.</p>
 *
 * <p>Status lifecycle: pending → accepted | rejected | cancelled</p>
 */
@Data
@Entity
@Table(name = "university_applications")
public class UniversityApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Raw FK columns (used for inserts/updates) ───────────────────────────

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "certificate_level_id")
    private Long certificateLevelId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "program_id")
    private Long programId;

    @Column(name = "personal_statement", columnDefinition = "TEXT")
    private String personalStatement;

    @Column(name = "status",
            nullable = false,
            columnDefinition = "ENUM('pending','accepted','rejected','cancelled')")
    private String status = "pending";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "acceptance_message", columnDefinition = "TEXT")
    private String acceptanceMessage;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Lazy relationships (read-only, for JPQL joins) ──────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    /** Maps to the institutions table — field named 'university' for semantic clarity. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", insertable = false, updatable = false)
    private Institution university;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_level_id", insertable = false, updatable = false)
    private CertificateLevel certificateLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", insertable = false, updatable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", insertable = false, updatable = false)
    private User reviewer;

    // ── Lifecycle ───────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) appliedAt = LocalDateTime.now();
    }
}
