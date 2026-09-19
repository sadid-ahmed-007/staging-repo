package com.eduauth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Maps to the verifier_access table.
 *
 * DB columns:
 *   id, verifier_id, student_id, request_id (FK to certificate_access_requests),
 *   granted_at, expires_at, revoked_at, revoked_by,
 *   created_at, updated_at, deleted_at
 */
@Entity
@Table(name = "verifier_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verifier_id", nullable = false)
    private Long verifierId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Optional certificate ID if this grant is for a specific certificate.
     * If null, this grant covers ALL certificates of this student.
     */
    @Column(name = "certificate_id")
    private Long certificateId;

    /** FK to the access request that produced this grant. */
    @Column(name = "request_id")
    private Long accessRequestId;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** user_id of whoever revoked (student or admin). */
    @Column(name = "revoked_by")
    private Long revokedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
