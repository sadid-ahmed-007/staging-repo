package com.eduauth.repository;

import com.eduauth.model.ProfileChangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileChangeRequestRepository extends JpaRepository<ProfileChangeRequest, Long>, JpaSpecificationExecutor<ProfileChangeRequest> {

    long countByStatus(String status);

    @Query("""
        SELECT p FROM ProfileChangeRequest p
        JOIN FETCH p.user u
        LEFT JOIN FETCH u.student
        LEFT JOIN FETCH u.institution
        LEFT JOIN FETCH u.verifier
        LEFT JOIN FETCH p.reviewer
        WHERE p.id = :id
        """)
    Optional<ProfileChangeRequest> findByIdWithDetails(@Param("id") Long id);

    List<ProfileChangeRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    boolean existsByUserIdAndFieldNameAndStatus(Long userId, String fieldName, String status);
}
