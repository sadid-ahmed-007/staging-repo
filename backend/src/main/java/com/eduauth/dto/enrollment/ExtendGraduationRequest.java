package com.eduauth.dto.enrollment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExtendGraduationRequest {

    @NotNull(message = "New expected graduation date is required")
    private LocalDate newExpectedGraduationDate;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String reason;
}
