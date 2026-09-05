package com.eduauth.repository;

import com.eduauth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    long countByRole(String role);
    long countByIsApprovedTrue();
    long countByIsApprovedFalseAndEmailVerifiedAtIsNotNull();
    
    // Pending approval queue: email verified (emailVerifiedAt IS NOT NULL) but not yet approved
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u WHERE u.isApproved = false AND u.emailVerifiedAt IS NOT NULL")
    List<User> findAllPendingApproval();
}
