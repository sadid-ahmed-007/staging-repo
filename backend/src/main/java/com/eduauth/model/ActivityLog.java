package com.eduauth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action;

    // Optional: type of entity this action relates to (e.g. "User", "Certificate")
    @Column(name = "entity_type")
    private String entityType;

    // Optional: PK of the related entity
    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // JSON metadata stored as raw string
    @Column(columnDefinition = "LONGTEXT")
    private String metadata;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // DB has updated_at on activity_logs
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
