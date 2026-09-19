package com.eduauth.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "certificate_sequences")
public class CertificateSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_key", nullable = false)
    private String sequenceKey;

    @Column(nullable = false)
    private String prefix = "BSC";

    @Column(name = "year_suffix", nullable = false)
    private String yearSuffix;

    @Column(name = "current_sequence", nullable = false)
    private Long currentSequence = 0L;

    @Column(name = "last_generated_at")
    private LocalDateTime lastGeneratedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
