package com.eduauth.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "certificate_access_requests")
public class AccessRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "verifier_id")
    private Long verifierId;

    @Column(name = "status", columnDefinition = "ENUM('pending','approved','rejected')")
    private String status;
}
