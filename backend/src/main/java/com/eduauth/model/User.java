package com.eduauth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    // Stored as ENUM in DB: 'student','university','verifier','admin'
    @Column(nullable = false, columnDefinition = "ENUM('student','university','verifier','admin')")
    private String role;

    // DB column: email_verified_at (timestamp, nullable — null = not verified)
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Pending email change flow
    @Column(name = "pending_email")
    private String pendingEmail;

    @Column(name = "pending_email_token")
    private String pendingEmailToken;

    @Column(name = "pending_email_expires_at")
    private LocalDateTime pendingEmailExpiresAt;

    // DB uses suspended_at timestamp — null means not suspended
    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(name = "remember_token")
    private String rememberToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft delete — Hibernate does NOT map this; kept for completeness
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Relationships (lazy — only loaded on explicit access) ───────────────
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Student student;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Institution institution;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Verifier verifier;

    // ── Business helper ─────────────────────────────────────────────────────

    /**
     * Returns true only if:
     *  - email has been verified (email_verified_at is not null)
     *  - account has been approved by admin
     *  - account is NOT suspended (suspended_at is null)
     */
    public boolean isAccountReady() {
        return emailVerifiedAt != null
                && Boolean.TRUE.equals(isApproved)
                && suspendedAt == null;
    }

    // ── UserDetails Implementation ──────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return suspendedAt == null;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
