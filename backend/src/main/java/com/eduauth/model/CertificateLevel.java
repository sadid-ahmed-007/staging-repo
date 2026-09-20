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
@Table(name = "certificate_levels")
public class CertificateLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @Column(nullable = false)
    private String name;

    @Column(name = "short_code", nullable = false)
    private String shortCode;

    /** Serial prefix used in certificate serial number generation (e.g. "BSC", "MBA", "PHD") */
    @Column(name = "serial_prefix", length = 10)
    private String serialPrefix;

    /** Duration of the program in years, used to auto-calculate expected graduation date */
    @Column(name = "duration_years", nullable = false)
    private Integer durationYears = 4;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Lazy-loaded departments under this certificate level (for program structure queries) */
    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_level_id", insertable = false, updatable = false)
    private List<Department> departments;
}
