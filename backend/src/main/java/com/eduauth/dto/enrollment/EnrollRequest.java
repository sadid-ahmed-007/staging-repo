package com.eduauth.dto.enrollment;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EnrollRequest {

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Student email is required")
    @JsonAlias({"student_email", "email"})
    private String studentEmail;

    @NotBlank(message = "Student ID in university is required")
    @JsonAlias({"student_id_in_university", "roll_number", "rollNumber", "student_id", "studentId"})
    private String studentIdInUniversity;

    @NotBlank(message = "Program is required")
    private String program;

    private String department;
    
    private String major;

    @JsonAlias({"certificate_level_id", "certificateLevelId"})
    private Long certificateLevelId;

    @JsonAlias({"department_id", "departmentId"})
    private Long departmentId;

    @JsonAlias({"major_id", "majorId"})
    private Long majorId;

    @JsonAlias({"batch_name"})
    private String batch;

    @NotNull(message = "Enrollment date is required")
    @JsonAlias({"enrollment_date"})
    private LocalDate enrollmentDate;

    @NotNull(message = "Expected graduation date is required")
    @JsonAlias({"expected_graduation_date"})
    private LocalDate expectedGraduationDate;
}
