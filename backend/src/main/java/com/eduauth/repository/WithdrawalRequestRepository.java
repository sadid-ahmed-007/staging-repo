package com.eduauth.repository;

import com.eduauth.model.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    @Query("SELECT COUNT(w) FROM WithdrawalRequest w WHERE w.enrollment.institution.id = :institutionId AND w.status = :status")
    long countByInstitutionIdAndStatus(@Param("institutionId") Long institutionId, @Param("status") String status);
}
