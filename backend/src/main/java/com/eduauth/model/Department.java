package com.eduauth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(name = "certificate_level_id")
    private Long certificateLevelId;

    @Column(nullable = false)
    private String name;

    /** Legacy column — kept for backwards compatibility */
    @Column(name = "short_code")
    private String shortCode;

    /** Department code used in the program structure API (e.g. "CSE", "EEE", "BBA") */
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Parent certificate level (lazy-loaded for program structure queries) */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_level_id", insertable = false, updatable = false)
    private CertificateLevel certificateLevel;

    /** Programs under this department (lazy-loaded for program structure queries) */
    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private List<Program> programs;
}
