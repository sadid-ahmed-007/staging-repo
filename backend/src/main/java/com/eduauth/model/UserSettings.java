package com.eduauth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // Stored as JSON string (LONGTEXT in DB)
    @Column(columnDefinition = "LONGTEXT")
    private String preferences;

    @Column(name = "profile_visibility", nullable = false)
    private String profileVisibility = "verifiers_only";

    @Column(name = "allow_verifier_search", nullable = false)
    private Boolean allowVerifierSearch = true;

    @Column(name = "show_email_to_verifiers", nullable = false)
    private Boolean showEmailToVerifiers = false;

    @Column(name = "show_institution_to_public", nullable = false)
    private Boolean showInstitutionToPublic = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
