package com.eduauth.repository;

import com.eduauth.model.AccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long>, JpaSpecificationExecutor<AccessGrant> {

    @Query("SELECT COUNT(a) FROM AccessGrant a WHERE a.studentId = :studentId AND a.expiresAt > :now AND a.revokedAt IS NULL")
    long countActiveGrantsForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a) FROM AccessGrant a WHERE a.verifierId = :verifierId AND a.expiresAt > :now AND a.revokedAt IS NULL")
    long countActiveGrantsForVerifier(@Param("verifierId") Long verifierId, @Param("now") LocalDateTime now);

    /** All grants for a verifier including expired/revoked (for history). */
    @Query("SELECT a FROM AccessGrant a WHERE a.verifierId = :verifierId ORDER BY a.grantedAt DESC")
    List<AccessGrant> findAllByVerifierId(@Param("verifierId") Long verifierId);

    /** All non-expired, non-revoked grants for a verifier (for the accessible-students list). */
    @Query("SELECT a FROM AccessGrant a WHERE a.verifierId = :verifierId AND a.expiresAt > :now AND a.revokedAt IS NULL ORDER BY a.expiresAt ASC")
    List<AccessGrant> findActiveGrantsForVerifier(@Param("verifierId") Long verifierId, @Param("now") LocalDateTime now);

    /** All non-expired, non-revoked grants between a verifier and student ordered by expiry descending. */
    @Query("""
        SELECT a FROM AccessGrant a
        WHERE a.verifierId = :verifierId
          AND a.studentId  = :studentId
          AND a.expiresAt  > :now
          AND a.revokedAt  IS NULL
        ORDER BY a.expiresAt DESC
        """)
    List<AccessGrant> findActiveGrantsList(
            @Param("verifierId") Long verifierId,
            @Param("studentId")  Long studentId,
            @Param("now")        LocalDateTime now);

    /** Check if a verifier has valid (active, not revoked) access to a specific student (safe first result). */
    default Optional<AccessGrant> findActiveGrant(Long verifierId, Long studentId, LocalDateTime now) {
        List<AccessGrant> list = findActiveGrantsList(verifierId, studentId, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ── Required by AccessService ────────────────────────────────────────────

    /** Active grant between a specific verifier and student (safe first result). */
    default Optional<AccessGrant> findByVerifierIdAndStudentIdAndRevokedAtIsNullAndExpiresAtAfter(
            Long verifierId, Long studentId, LocalDateTime now) {
        return findActiveGrant(verifierId, studentId, now);
    }

    /** Find access grant created for a specific access request. */
    Optional<AccessGrant> findByAccessRequestId(Long accessRequestId);

    /** All non-expired, non-revoked grants for a student (verifiers currently with access). */
    @Query("""
        SELECT a FROM AccessGrant a
        WHERE a.studentId = :studentId
          AND a.revokedAt IS NULL
          AND a.expiresAt > :now
        ORDER BY a.expiresAt DESC
        """)
    List<AccessGrant> findByStudentIdAndRevokedAtIsNullAndExpiresAtAfter(
            @Param("studentId") Long studentId,
            @Param("now")       LocalDateTime now);

    /** All grants for a student including expired/revoked (for history). */
    @Query("""
        SELECT a FROM AccessGrant a
        WHERE a.studentId = :studentId
        ORDER BY a.grantedAt DESC
        """)
    List<AccessGrant> findAllByStudentId(@Param("studentId") Long studentId);

    /** All non-expired, non-revoked grants for a verifier. */
    @Query("""
        SELECT a FROM AccessGrant a
        WHERE a.verifierId = :verifierId
          AND a.revokedAt  IS NULL
          AND a.expiresAt  > :now
        ORDER BY a.expiresAt DESC
        """)
    List<AccessGrant> findByVerifierIdAndRevokedAtIsNullAndExpiresAtAfter(
            @Param("verifierId") Long verifierId,
            @Param("now")        LocalDateTime now);

    /** Existence check — does this verifier have active access to this student? */
    @Query("""
        SELECT COUNT(a) > 0 FROM AccessGrant a
        WHERE a.verifierId = :verifierId
          AND a.studentId  = :studentId
          AND a.revokedAt  IS NULL
          AND a.expiresAt  > :now
        """)
    boolean existsByVerifierIdAndStudentIdAndRevokedAtIsNullAndExpiresAtAfter(
            @Param("verifierId") Long verifierId,
            @Param("studentId")  Long studentId,
            @Param("now")        LocalDateTime now);

    /** Check if an active grant for ALL certificates exists between verifier and student. */
    @Query("""
        SELECT COUNT(a) > 0 FROM AccessGrant a
        WHERE a.verifierId = :verifierId
          AND a.studentId  = :studentId
          AND a.certificateId IS NULL
          AND a.revokedAt  IS NULL
          AND a.expiresAt  > :now
        """)
    boolean existsActiveGrantForAllCertificates(
            @Param("verifierId") Long verifierId,
            @Param("studentId")  Long studentId,
            @Param("now")        LocalDateTime now);

    /** Find active grants that cover a specific certificate (either specific or all-certificates grant). */
    @Query("""
        SELECT a FROM AccessGrant a
        WHERE a.verifierId = :verifierId
          AND a.studentId  = :studentId
          AND (a.certificateId IS NULL OR a.certificateId = :certificateId)
          AND a.revokedAt  IS NULL
          AND a.expiresAt  > :now
        ORDER BY a.expiresAt DESC
        """)
    List<AccessGrant> findActiveGrantsForCertificate(
            @Param("verifierId")    Long verifierId,
            @Param("studentId")     Long studentId,
            @Param("certificateId") Long certificateId,
            @Param("now")           LocalDateTime now);

    /** Find all active grants between a verifier and student. */
    @Query("""
        SELECT a FROM AccessGrant a
        WHERE a.verifierId = :verifierId
          AND a.studentId  = :studentId
          AND a.revokedAt  IS NULL
          AND a.expiresAt  > :now
        ORDER BY a.expiresAt DESC
        """)
    List<AccessGrant> findAllActiveGrantsForVerifierAndStudent(
            @Param("verifierId") Long verifierId,
            @Param("studentId")  Long studentId,
            @Param("now")        LocalDateTime now);
}
