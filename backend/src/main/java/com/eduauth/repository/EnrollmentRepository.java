package com.eduauth.repository;

import com.eduauth.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findFirstByStudentIdAndStatusOrderByEnrollmentDateDesc(Long studentId, String status);
    long countByInstitutionIdAndStatus(Long institutionId, String status);
}
