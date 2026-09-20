package com.eduauth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents an academic program within a department.
 * Sits in the hierarchy: CertificateLevel → Department → Program.
 *
 * <p>The program name is the full certificate name (e.g.
 * "Bachelor of Science in Computer Science and Engineering").
 * The shortName is a UI-friendly label (e.g. "BSc in CSE").</p>
 */
@Data
@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    /** Full program name — used as certificate_name when issuing a certificate */
    @Column(nullable = false)
    private String name;

    /** Short display name, e.g. "BSc in CSE" */
    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Parent department — lazy-loaded for navigation queries */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;

    /** Parent institution */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", insertable = false, updatable = false)
    private Institution institution;
}
