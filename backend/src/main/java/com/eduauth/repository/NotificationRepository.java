package com.eduauth.repository;

import com.eduauth.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    // ── Paginated: all notifications for a user, newest first ─────────────────
    @Query("SELECT n FROM Notification n WHERE n.notifiableId = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ── Unread only, paginated ─────────────────────────────────────────────────
    @Query("SELECT n FROM Notification n WHERE n.notifiableId = :userId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ── Unread only, list (for mark-all-read) ─────────────────────────────────
    @Query("SELECT n FROM Notification n WHERE n.notifiableId = :userId AND n.readAt IS NULL")
    List<Notification> findByUserIdAndReadAtIsNull(@Param("userId") Long userId);

    // ── Count unread ───────────────────────────────────────────────────────────
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.notifiableId = :userId AND n.readAt IS NULL")
    long countByUserIdAndReadAtIsNull(@Param("userId") Long userId);

    // ── Latest N notifications regardless of read status (for dropdown) ────────
    @Query("SELECT n FROM Notification n WHERE n.notifiableId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findLatestByUserId(@Param("userId") Long userId, Pageable pageable);

    // ── Read only, paginated ───────────────────────────────────────────────────
    @Query("SELECT n FROM Notification n WHERE n.notifiableId = :userId AND n.readAt IS NOT NULL ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndReadAtIsNotNullOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ── Mark all unread as read ────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now, n.updatedAt = :now WHERE n.notifiableId = :userId AND n.readAt IS NULL")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
