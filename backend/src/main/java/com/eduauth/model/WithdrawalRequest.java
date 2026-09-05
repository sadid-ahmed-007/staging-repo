package com.eduauth.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @Column(name = "status", columnDefinition = "ENUM('pending','approved','rejected')")
    private String status;
}
