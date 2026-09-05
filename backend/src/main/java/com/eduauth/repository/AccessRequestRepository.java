package com.eduauth.repository;

import com.eduauth.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    long countByStudentIdAndStatus(Long studentId, String status);
    long countByVerifierIdAndStatus(Long verifierId, String status);
}
