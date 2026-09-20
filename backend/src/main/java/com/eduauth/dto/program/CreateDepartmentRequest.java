package com.eduauth.dto.program;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateDepartmentRequest {

    @NotNull(message = "Certificate level ID is required")
    private Long certificateLevelId;

    @NotBlank(message = "Department name is required")
    @Size(max = 150, message = "Department name must not exceed 150 characters")
    private String name;

    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String code;
}
