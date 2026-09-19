package com.eduauth.repository;

import com.eduauth.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
