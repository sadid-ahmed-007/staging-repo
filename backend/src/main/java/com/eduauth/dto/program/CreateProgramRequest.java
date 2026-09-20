package com.eduauth.dto.program;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProgramRequest {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotBlank(message = "Program name is required")
    @Size(max = 150, message = "Program name must not exceed 150 characters")
    private String name;

    @Size(max = 100, message = "Short name must not exceed 100 characters")
    private String shortName;
}
