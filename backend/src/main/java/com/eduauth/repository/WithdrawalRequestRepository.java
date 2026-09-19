package com.eduauth.repository;

import com.eduauth.model.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    Optional<WithdrawalRequest> findFirstByEnrollmentIdAndStatusOrderByCreatedAtDesc(
            Long enrollmentId, String status);

    List<WithdrawalRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<WithdrawalRequest> findFirstByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<WithdrawalRequest> findFirstByStudentIdAndStatusOrderByCreatedAtDesc(
            Long studentId, String status);

    boolean existsByEnrollmentIdAndStatus(Long enrollmentId, String status);

    @Query("SELECT COUNT(w) FROM WithdrawalRequest w WHERE w.enrollment.institutionId = :institutionId AND w.status = :status")
    long countByInstitutionIdAndStatus(@Param("institutionId") Long institutionId, @Param("status") String status);
}
