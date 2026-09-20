package com.eduauth.repository;

import com.eduauth.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByUserId(Long userId);

    Optional<Institution> findByUserEmail(String email);

    @org.springframework.data.jpa.repository.Query("""
            SELECT i FROM Institution i
            JOIN i.user u
            WHERE u.isApproved = true
              AND u.suspendedAt IS NULL
              AND i.deletedAt IS NULL
              AND (
                  :search IS NULL OR :search = '' 
                  OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(i.city) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY i.name ASC
            """)
    java.util.List<Institution> searchApprovedInstitutions(@org.springframework.data.repository.query.Param("search") String search);
}
