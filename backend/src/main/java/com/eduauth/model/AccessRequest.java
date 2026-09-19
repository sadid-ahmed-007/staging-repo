package com.eduauth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Maps to the certificate_access_requests table.
 *
 * DB columns:
 *   id, verifier_id, student_id, purpose, status, responded_at,
 *   rejection_reason (used as responseMessage), access_duration_days
 *   (stores BOTH the requested duration at submit time AND the granted duration),
 *   created_at, updated_at, deleted_at
 *
 * Status values in DB ENUM: pending, approved, rejected
 * Note: 'cancelled' is not in the DB ENUM — soft-deleted rows represent cancellations.
 * We store 'cancelled' by setting deleted_at instead of changing status,
 * OR we handle it purely in application logic by filtering.
 * For simplicity and DB compatibility: cancelled requests are soft-deleted (deleted_at set).
 */
@Entity
@Table(name = "certificate_access_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verifier_id", nullable = false)
    private Long verifierId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Optional certificate ID if this access request is for a specific certificate.
     * If null, the request is for ALL certificates of this student.
     */
    @Column(name = "certificate_id")
    private Long certificateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    /**
     * Requested duration in days — submitted by the verifier when creating the request.
     * Also reused (via access_duration_days) to store the approved duration when approved.
     */
    @Column(name = "access_duration_days")
    private Integer requestedDurationDays;

    /**
     * Status: pending | approved | rejected
     * (cancelled is represented by soft-delete / deletedAt != null)
     */
    @Column(columnDefinition = "ENUM('pending','approved','rejected')")
    private String status = "pending";

    /** Set when the student responds (approved or rejected). */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /**
     * Student's response message.
     * Stored in rejection_reason column for both approved and rejected cases.
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String responseMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Soft delete. When set, the request is effectively "cancelled". */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
