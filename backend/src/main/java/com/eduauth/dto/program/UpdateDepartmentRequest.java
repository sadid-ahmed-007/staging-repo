package com.eduauth.dto.program;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDepartmentRequest {

    @Size(max = 150, message = "Department name must not exceed 150 characters")
    private String name;

    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String code;

    private Boolean isActive;
}
