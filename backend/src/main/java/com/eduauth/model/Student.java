package com.eduauth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    // DB: nid_hash VARCHAR(64) NOT NULL UNIQUE
    @Column(name = "nid_hash", nullable = false, unique = true)
    private String nidHash;

    // DB: nid_encrypted TEXT (nullable)
    @Column(name = "nid_encrypted", columnDefinition = "TEXT")
    private String nidEncrypted;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    // DB: gender ENUM('Male','Female','Other') nullable — columnDefinition tells Hibernate
    // the exact type so schema validation passes (MySQL reports ENUMs as CHAR)
    @Column(columnDefinition = "ENUM('Male','Female','Other')")
    private String gender;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft delete column — not managed by Hibernate, kept for schema compliance
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
