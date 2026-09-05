package com.eduauth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores a pending registration before email is verified.
 * The user's full registration data (as JSON) is kept here alongside
 * a SHA-256 hash of the 6-digit OTP.
 *
 * On successful verification, a real User + profile record is created
 * and this row is marked as verified (verified_at set).
 */
@Entity
@Table(name = "pending_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "registration_role", nullable = false)
    private String registrationRole;

    // SHA-256 hash of the plain OTP code (never store the raw OTP)
    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    // Full registration payload as JSON string
    @Column(name = "registration_data", nullable = false, columnDefinition = "LONGTEXT")
    private String registrationData;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private Integer attempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
