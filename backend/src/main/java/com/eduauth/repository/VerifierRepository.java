package com.eduauth.repository;

import com.eduauth.model.Verifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerifierRepository extends JpaRepository<Verifier, Long> {

    Optional<Verifier> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Verifier v JOIN FETCH v.user WHERE v.id = :id")
    Optional<Verifier> findByIdWithUser(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Verifier v JOIN FETCH v.user WHERE v.user.id = :userId")
    Optional<Verifier> findByUserIdWithUser(@org.springframework.data.repository.query.Param("userId") Long userId);
}
