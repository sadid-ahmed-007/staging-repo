package com.eduauth.repository;

import com.eduauth.model.AccountDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, String status);

    Optional<AccountDeletionRequest> findByUserIdAndStatus(Long userId, String status);

    List<AccountDeletionRequest> findByStatusOrderByRequestedAtDesc(String status);
}
