package com.eduauth.repository;

import com.eduauth.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByInstitutionId(Long institutionId);
    List<Department> findByInstitutionIdAndCertificateLevelId(Long institutionId, Long certificateLevelId);
    Optional<Department> findByIdAndInstitutionId(Long id, Long institutionId);
}
