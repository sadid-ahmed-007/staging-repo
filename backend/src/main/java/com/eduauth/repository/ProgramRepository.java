package com.eduauth.repository;

import com.eduauth.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findByDepartmentId(Long departmentId);

    List<Program> findByDepartmentIdAndIsActiveTrue(Long departmentId);

    List<Program> findByUniversityId(Long universityId);

    List<Program> findByUniversityIdAndIsActiveTrue(Long universityId);

    Optional<Program> findByIdAndUniversityId(Long id, Long universityId);

    boolean existsByDepartmentId(Long departmentId);

    /** Load programs with their department and department's certificateLevel in one query */
    @Query("SELECT p FROM Program p " +
           "JOIN FETCH p.department d " +
           "JOIN FETCH d.certificateLevel cl " +
           "WHERE p.universityId = :universityId AND p.isActive = true " +
           "ORDER BY cl.name, d.name, p.name")
    List<Program> findActiveByUniversityWithHierarchy(@Param("universityId") Long universityId);
}
