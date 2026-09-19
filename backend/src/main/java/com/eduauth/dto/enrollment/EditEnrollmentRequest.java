package com.eduauth.dto.enrollment;

import lombok.Data;

import java.time.LocalDate;

/**
 * All fields optional — only non-null fields are applied.
 */
@Data
public class EditEnrollmentRequest {
    private String program;
    private String department;
    private String major;
    private Long certificateLevelId;
    private Long departmentId;
    private Long majorId;
    private String batch;
    private LocalDate expectedGraduationDate;
}
