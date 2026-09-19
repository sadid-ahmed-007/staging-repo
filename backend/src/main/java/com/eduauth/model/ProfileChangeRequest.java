package com.eduauth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProfileChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "current_value", columnDefinition = "TEXT")
    private String currentValue;

    @Column(name = "requested_value", nullable = false, columnDefinition = "TEXT")
    private String requestedValue;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, columnDefinition = "ENUM('pending','approved','rejected')")
    private String status = "pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "supporting_documents", columnDefinition = "JSON")
    private String supportingDocuments;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isPending() {
        return "pending".equalsIgnoreCase(this.status);
    }
}
