package com.eduauth.repository;

import com.eduauth.model.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    // ── Existing counts used by dashboards ───────────────────────────────────
    long countByStudentId(Long studentId);
    long countByStudentIdAndIsPubliclyShareableTrue(Long studentId);
    long countByStudentIdAndIsPubliclyShareableFalse(Long studentId);
    long countByInstitutionId(Long institutionId);
    long countByInstitutionIdAndIssueDateBetween(Long institutionId, LocalDate start, LocalDate end);

    // ── Lookup by serial (public verify + student PDF) ────────────────────────
    Optional<Certificate> findBySerial(String serial);
    boolean existsBySerial(String serial);

    @Query("""
        SELECT c FROM Certificate c
        LEFT JOIN FETCH c.student s
        LEFT JOIN FETCH s.user
        LEFT JOIN FETCH c.institution
        LEFT JOIN FETCH c.enrollment
        WHERE c.id = :id
        """)
    Optional<Certificate> findByIdWithDetails(@Param("id") Long id);

    // ── Student view ──────────────────────────────────────────────────────────
    List<Certificate> findByStudentIdOrderByIssueDateDesc(Long studentId);

    @Query("""
        SELECT c FROM Certificate c
        LEFT JOIN FETCH c.institution
        WHERE c.studentId = :studentId
          AND (:filter = 'public'  AND c.isPubliclyShareable = true
               OR :filter = 'private' AND c.isPubliclyShareable = false
               OR :filter = 'all')
        ORDER BY c.issueDate DESC
        """)
    Page<Certificate> findByStudentIdWithFilter(
            @Param("studentId") Long studentId,
            @Param("filter") String filter,
            Pageable pageable);

    // ── University view ────────────────────────────────────────────────────────
    @Query("""
        SELECT c FROM Certificate c
          LEFT JOIN FETCH c.student s
          LEFT JOIN FETCH s.user u
        WHERE c.institutionId = :institutionId
          AND (:level IS NULL OR :level = 'all' OR c.certificateLevel = :level)
          AND (:monthStart IS NULL OR c.issueDate >= :monthStart)
          AND (:monthEnd   IS NULL OR c.issueDate <= :monthEnd)
          AND (
               :search IS NULL OR :search = ''
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.issuedName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.serial) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY c.issueDate DESC
        """)
    Page<Certificate> findByInstitutionWithFilters(
            @Param("institutionId") Long institutionId,
            @Param("level") String level,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("search") String search,
            Pageable pageable);

    // ── Admin view ─────────────────────────────────────────────────────────────
    @Query("""
        SELECT c FROM Certificate c
          LEFT JOIN FETCH c.student s
          LEFT JOIN FETCH s.user u
          LEFT JOIN FETCH c.institution inst
        WHERE
          (:status = 'all' OR :status IS NULL
           OR (:status = 'active'  AND c.revokedAt IS NULL)
           OR (:status = 'revoked' AND c.revokedAt IS NOT NULL))
          AND (:institutionId IS NULL OR c.institutionId = :institutionId)
          AND (
               :search IS NULL OR :search = ''
               OR LOWER(c.issuedName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.serial) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(inst.name) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY c.createdAt DESC
        """)
    Page<Certificate> findAllWithFilters(
            @Param("status") String status,
            @Param("institutionId") Long institutionId,
            @Param("search") String search,
            Pageable pageable);

    // ── Verifier view — all certs for a given student ─────────────────────────
    List<Certificate> findByStudentIdAndRevokedAtIsNullOrderByIssueDateDesc(Long studentId);
}
