package com.eduauth.repository;

import com.eduauth.model.Verifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerifierRepository extends JpaRepository<Verifier, Long> {

    Optional<Verifier> findByUserId(Long userId);
}
