package com.eduauth.repository;

import com.eduauth.model.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    Optional<PendingRegistration> findByEmailAndVerifiedAtIsNull(String email);
    void deleteByEmail(String email);
}
