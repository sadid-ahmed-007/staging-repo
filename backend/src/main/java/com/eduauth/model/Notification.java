package com.eduauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Maps to the `notifications` table (Laravel morphable format).
 *
 * The table uses:
 *   - id             CHAR(36) UUID primary key
 *   - type           VARCHAR  (e.g. "App\\Notifications\\AppNotification")
 *   - notifiable_type VARCHAR (always "App\\Models\\User")
 *   - notifiable_id  BIGINT   (the user_id receiving the notification)
 *   - data           TEXT     (JSON blob containing type, title, message, action_url, data fields)
 *   - read_at        TIMESTAMP NULL
 *   - created_at     TIMESTAMP NULL
 *   - updated_at     TIMESTAMP NULL
 *
 * The "logical" fields (title, message, actionUrl, subtype) are embedded inside the data JSON.
 * We keep them as transient fields for convenience after deserialization.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", nullable = false, updatable = false)
    private String id;

    /** Laravel notification class path — always "App\\Notifications\\AppNotification" */
    @Column(name = "type", nullable = false)
    private String type;

    /** Morphable type — always "App\\Models\\User" */
    @Column(name = "notifiable_type", nullable = false)
    private String notifiableType;

    /** The user_id this notification belongs to */
    @Column(name = "notifiable_id", nullable = false)
    private Long notifiableId;

    /**
     * JSON blob stored in DB.
     * Format: { "type": "CERT_ISSUED", "title": "...", "message": "...",
     *           "action_url": "...", "data": { ... } }
     */
    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Transient convenience fields (populated by NotificationService after parsing) ──

    @Transient
    private String notificationType;   // e.g. "CERTIFICATE_ISSUED"

    @Transient
    private String title;

    @Transient
    private String message;

    @Transient
    private String actionUrl;

    @Transient
    private String dataJson;           // the nested "data" sub-object as JSON string

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
