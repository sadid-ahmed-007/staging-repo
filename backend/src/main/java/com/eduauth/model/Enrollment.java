package com.eduauth.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_number", unique = true, nullable = false)
    private String enrollmentNumber;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "institution_id")
    private Long institutionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    /** Roll number / Student ID assigned by the university */
    @Column(name = "roll_number")
    private String rollNumber;

    private String program;
    private String batch;

    @Column(name = "status",
            columnDefinition = "ENUM('active','graduated','suspended','withdrawn')")
    private String status;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "expected_graduation_date")
    private LocalDate expectedGraduationDate;

    @Column(name = "actual_graduation_date")
    private LocalDate actualGraduationDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
