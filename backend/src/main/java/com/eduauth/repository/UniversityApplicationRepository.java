package com.eduauth.repository;

import com.eduauth.model.UniversityApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UniversityApplicationRepository extends JpaRepository<UniversityApplication, Long> {

    // ── Student queries ───────────────────────────────────────────────────────

    /**
     * All applications for a student, optionally filtered by status.
     * Pass null for status to get all.
     */
    @Query("""
            SELECT a FROM UniversityApplication a
            WHERE a.studentId = :studentId
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.appliedAt DESC
            """)
    Page<UniversityApplication> findByStudentId(
            @Param("studentId") Long studentId,
            @Param("status") String status,
            Pageable pageable);

    // ── University queries ────────────────────────────────────────────────────

    /**
     * All applications to a university, optionally filtered by status.
     * Joins student for richer ordering capability.
     */
    @Query("""
            SELECT a FROM UniversityApplication a
            JOIN a.student s
            WHERE a.universityId = :universityId
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.appliedAt DESC
            """)
    Page<UniversityApplication> findByUniversityId(
            @Param("universityId") Long universityId,
            @Param("status") String status,
            Pageable pageable);

    // ── Existence / uniqueness checks + enrollment integration ─────────────────

    /**
     * Used to:
     * 1. Detect duplicate pending applications (service-layer uniqueness enforcement).
     * 2. Check if a student has an accepted application to a specific university
     *    (used by search-to-enroll for hasAcceptedApplication badge).
     */
    boolean existsByStudentIdAndUniversityIdAndStatus(
            Long studentId, Long universityId, String status);

    Optional<UniversityApplication> findByStudentIdAndUniversityIdAndStatus(
            Long studentId, Long universityId, String status);

    /**
     * Count accepted applications for a student at a specific university.
     * Used to show the "Application approved" badge in search results.
     */
    @Query("""
            SELECT COUNT(a) FROM UniversityApplication a
            WHERE a.studentId = :studentId
              AND a.universityId = :universityId
              AND a.status = 'accepted'
            """)
    long countAcceptedByStudentAndUniversity(
            @Param("studentId") Long studentId,
            @Param("universityId") Long universityId);

    /**
     * Accepted applications to this university that have NOT yet been turned
     * into an active enrollment. Used for the "From Application" tab.
     */
    @Query("""
            SELECT a FROM UniversityApplication a
            JOIN a.student s
            WHERE a.universityId = :universityId
              AND a.status = 'accepted'
              AND NOT EXISTS (
                  SELECT e FROM Enrollment e
                  WHERE e.studentId = a.studentId
                    AND e.institutionId = :universityId
                    AND e.status = 'active'
                    AND e.deletedAt IS NULL
              )
            ORDER BY a.appliedAt DESC
            """)
    java.util.List<UniversityApplication> findAcceptedNotYetEnrolled(
            @Param("universityId") Long universityId);
}
