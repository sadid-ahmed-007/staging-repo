package com.eduauth.repository;

import com.eduauth.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByInstitutionId(Long institutionId);

    List<Enrollment> findByStudentIdAndStatus(Long studentId, String status);

    boolean existsByStudentIdAndStatus(Long studentId, String status);

    boolean existsByCertificateLevelIdAndStatus(Long certificateLevelId, String status);

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    List<Enrollment> findByInstitutionIdAndStatus(Long institutionId, String status);

    List<Enrollment> findByStudentIdAndInstitutionId(Long studentId, Long institutionId);

    Optional<Enrollment> findFirstByStudentIdAndStatusOrderByEnrollmentDateDesc(Long studentId, String status);

    long countByInstitutionIdAndStatus(Long institutionId, String status);

    long countByInstitutionId(Long institutionId);

    long countByStatus(String status);

    /** Paginated enrollment list with optional status and search filters */
    @Query("""
            SELECT e FROM Enrollment e
            JOIN e.student s
            WHERE e.institutionId = :institutionId
              AND (:status IS NULL OR e.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(s.firstName) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(s.lastName) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(e.enrollmentNumber) LIKE LOWER(CONCAT('%',:search,'%')))
              AND e.deletedAt IS NULL
            ORDER BY e.createdAt DESC
            """)
    Page<Enrollment> findByInstitutionWithFilters(
            @Param("institutionId") Long institutionId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    /** Find active or pending-withdrawal enrollments for a student (for student dashboard) */
    @Query("""
            SELECT e FROM Enrollment e
            LEFT JOIN FETCH e.institution
            WHERE e.studentId = :studentId
              AND e.status = 'active'
              AND e.deletedAt IS NULL
            ORDER BY e.createdAt DESC
            """)
    List<Enrollment> findAllActiveByStudentId(@Param("studentId") Long studentId);

    default Optional<Enrollment> findActiveByStudentId(Long studentId) {
        List<Enrollment> list = findAllActiveByStudentId(studentId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Pending withdrawal: enrollments at this institution where student has a pending withdrawal request */
    @Query("""
            SELECT e FROM Enrollment e
            WHERE e.institutionId = :institutionId
              AND e.status = 'active'
              AND e.deletedAt IS NULL
              AND EXISTS (
                  SELECT w FROM WithdrawalRequest w
                  WHERE w.enrollmentId = e.id AND w.status = 'pending'
              )
            ORDER BY e.createdAt DESC
            """)
    List<Enrollment> findWithPendingWithdrawalByInstitutionId(@Param("institutionId") Long institutionId);

    /** Search active enrolled students at an institution by name, email, roll number, or enrollment number */
    @Query("""
            SELECT e FROM Enrollment e
            JOIN FETCH e.student s
            JOIN FETCH s.user u
            WHERE e.institutionId = :institutionId
              AND e.status = 'active'
              AND e.deletedAt IS NULL
              AND (:search IS NULL OR :search = ''
                   OR LOWER(s.firstName) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(s.lastName) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(e.rollNumber) LIKE LOWER(CONCAT('%',:search,'%'))
                   OR LOWER(e.enrollmentNumber) LIKE LOWER(CONCAT('%',:search,'%')))
            ORDER BY e.createdAt DESC
            """)
    List<Enrollment> searchEnrolledStudents(
            @Param("institutionId") Long institutionId,
            @Param("search") String search,
            Pageable pageable);

    /** Exact case-insensitive match on roll number (university-assigned student ID). */
    Optional<Enrollment> findFirstByRollNumberIgnoreCase(String rollNumber);

    /** Exact case-insensitive match on enrollment number. */
    Optional<Enrollment> findFirstByEnrollmentNumberIgnoreCase(String enrollmentNumber);
}
