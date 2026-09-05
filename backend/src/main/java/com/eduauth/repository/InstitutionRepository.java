package com.eduauth.repository;

import com.eduauth.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByUserId(Long userId);

    Optional<Institution> findByUserEmail(String email);
}
