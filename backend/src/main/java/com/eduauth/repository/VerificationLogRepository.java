package com.eduauth.repository;

import com.eduauth.model.VerificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {

    // ── Stats ───────────────────────────────────────────────────────────────
    long countByVerifierId(Long verifierId);

    long countByVerifierIdAndVerificationResult(Long verifierId, String result);

    long countByVerifierIdAndCreatedAtBetween(Long verifierId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(v) FROM VerificationLog v WHERE v.verifierId = :uid AND v.verificationResult <> 'success'")
    long countFailedByVerifierId(@Param("uid") Long verifierId);

    @Query("SELECT COUNT(v) FROM VerificationLog v WHERE v.verifierId = :uid AND v.verifiedAt >= :start AND v.verifiedAt < :end")
    long countByVerifierIdToday(@Param("uid") Long verifierId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ── Recent ──────────────────────────────────────────────────────────────
    List<VerificationLog> findTop10ByVerifierIdOrderByVerifiedAtDesc(Long verifierId);

    // ── Paginated history ───────────────────────────────────────────────────
    Page<VerificationLog> findByVerifierId(Long verifierId, Pageable pageable);

    @Query("SELECT v FROM VerificationLog v WHERE v.verifierId = :uid " +
           "AND (:status IS NULL OR v.verificationResult = :status) " +
           "AND (:serial IS NULL OR v.serial LIKE CONCAT('%', :serial, '%')) " +
           "AND (v.verifiedAt >= :fromDate) " +
           "AND (v.verifiedAt <= :toDate) " +
           "ORDER BY v.verifiedAt DESC")
    Page<VerificationLog> findFilteredHistory(
            @Param("uid") Long verifierId,
            @Param("status") String status,
            @Param("serial") String serial,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    // ── Export (all matching, no pagination) ─────────────────────────────────
    @Query("SELECT v FROM VerificationLog v WHERE v.verifierId = :uid " +
           "AND (:status IS NULL OR v.verificationResult = :status) " +
           "AND (:serial IS NULL OR v.serial LIKE CONCAT('%', :serial, '%')) " +
           "AND (v.verifiedAt >= :fromDate) " +
           "AND (v.verifiedAt <= :toDate) " +
           "ORDER BY v.verifiedAt DESC")
    List<VerificationLog> findFilteredForExport(
            @Param("uid") Long verifierId,
            @Param("status") String status,
            @Param("serial") String serial,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
