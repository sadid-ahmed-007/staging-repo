package com.eduauth.repository;

import com.eduauth.model.AccessRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    long countByStudentIdAndStatus(Long studentId, String status);

    long countByVerifierIdAndStatus(Long verifierId, String status);

    // ── Required by AccessService ────────────────────────────────────────────

    Optional<AccessRequest> findByVerifierIdAndStudentIdAndStatus(
            Long verifierId, Long studentId, String status);

    /**
     * Paginated requests received by a student — excludes soft-deleted (cancelled) rows.
     * Pass null status to get all non-deleted requests.
     */
    @Query("""
            SELECT r FROM AccessRequest r
            WHERE r.studentId = :studentId
              AND r.deletedAt IS NULL
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<AccessRequest> findByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status")    String status,
            Pageable pageable);

    /**
     * Paginated requests sent by a verifier — includes soft-deleted rows mapped
     * as 'cancelled' in the service layer.
     */
    @Query("""
            SELECT r FROM AccessRequest r
            WHERE r.verifierId = :verifierId
            ORDER BY r.createdAt DESC
            """)
    Page<AccessRequest> findByVerifierIdOrderByRequestedAtDesc(
            @Param("verifierId") Long verifierId,
            Pageable pageable);

    /**
     * Active pending requests sent by a verifier (not soft-deleted).
     */
    @Query("""
            SELECT r FROM AccessRequest r
            WHERE r.verifierId = :verifierId
              AND r.status = :status
              AND r.deletedAt IS NULL
            ORDER BY r.createdAt DESC
            """)
    Page<AccessRequest> findByVerifierIdAndStatusActive(
            @Param("verifierId") Long verifierId,
            @Param("status")     String status,
            Pageable pageable);

    boolean existsByVerifierIdAndStudentIdAndStatus(
            Long verifierId, Long studentId, String status);

    /**
     * Check for an existing pending (non-soft-deleted) request.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM AccessRequest r
            WHERE r.verifierId = :verifierId
              AND r.studentId  = :studentId
              AND r.status     = 'pending'
              AND r.deletedAt  IS NULL
            """)
    boolean existsPendingRequest(
            @Param("verifierId") Long verifierId,
            @Param("studentId")  Long studentId);

    /**
     * Check for an existing pending request specifically for ALL certificates.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM AccessRequest r
            WHERE r.verifierId = :verifierId
              AND r.studentId  = :studentId
              AND r.certificateId IS NULL
              AND r.status     = 'pending'
              AND r.deletedAt  IS NULL
            """)
    boolean existsPendingRequestForAll(
            @Param("verifierId") Long verifierId,
            @Param("studentId")  Long studentId);

    /**
     * Check for an existing pending request for a specific certificate.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM AccessRequest r
            WHERE r.verifierId = :verifierId
              AND r.studentId  = :studentId
              AND r.certificateId = :certificateId
              AND r.status     = 'pending'
              AND r.deletedAt  IS NULL
            """)
    boolean existsPendingRequestForCertificate(
            @Param("verifierId")    Long verifierId,
            @Param("studentId")     Long studentId,
            @Param("certificateId") Long certificateId);

    /**
     * Find a specific pending request belonging to a verifier (not soft-deleted).
     */
    @Query("""
            SELECT r FROM AccessRequest r
            WHERE r.id        = :id
              AND r.verifierId = :verifierId
              AND r.status     = 'pending'
              AND r.deletedAt  IS NULL
            """)
    Optional<AccessRequest> findPendingByIdAndVerifierId(
            @Param("id")         Long id,
            @Param("verifierId") Long verifierId);

    /**
     * Find a specific pending request belonging to a student.
     */
    @Query("""
            SELECT r FROM AccessRequest r
            WHERE r.id       = :id
              AND r.studentId = :studentId
              AND r.status    = 'pending'
              AND r.deletedAt IS NULL
            """)
    Optional<AccessRequest> findPendingByIdAndStudentId(
            @Param("id")        Long id,
            @Param("studentId") Long studentId);
}
