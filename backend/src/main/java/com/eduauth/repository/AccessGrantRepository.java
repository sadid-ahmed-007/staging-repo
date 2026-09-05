package com.eduauth.repository;

import com.eduauth.model.AccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {

    @Query("SELECT COUNT(a) FROM AccessGrant a WHERE a.studentId = :studentId AND a.expiresAt > :now AND a.revokedAt IS NULL")
    long countActiveGrantsForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a) FROM AccessGrant a WHERE a.verifierId = :verifierId AND a.expiresAt > :now AND a.revokedAt IS NULL")
    long countActiveGrantsForVerifier(@Param("verifierId") Long verifierId, @Param("now") LocalDateTime now);

    /** All non-expired, non-revoked grants for a verifier (for the accessible-students list). */
    @Query("SELECT a FROM AccessGrant a WHERE a.verifierId = :verifierId AND a.expiresAt > :now AND a.revokedAt IS NULL ORDER BY a.expiresAt ASC")
    List<AccessGrant> findActiveGrantsForVerifier(@Param("verifierId") Long verifierId, @Param("now") LocalDateTime now);

    /** Check if a verifier has valid (active, not revoked) access to a specific student. */
    @Query("""
        SELECT a FROM AccessGrant a
        WHERE a.verifierId = :verifierId
          AND a.studentId  = :studentId
          AND a.expiresAt  > :now
          AND a.revokedAt  IS NULL
        ORDER BY a.expiresAt DESC
        """)
    Optional<AccessGrant> findActiveGrant(
            @Param("verifierId") Long verifierId,
            @Param("studentId")  Long studentId,
            @Param("now")        LocalDateTime now);
}
