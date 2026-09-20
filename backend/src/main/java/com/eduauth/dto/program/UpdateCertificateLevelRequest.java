package com.eduauth.dto.program;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateCertificateLevelRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Short name must not exceed 50 characters")
    private String shortName;

    @Size(max = 20, message = "Serial prefix must not exceed 20 characters")
    private String serialPrefix;

    @Min(value = 1, message = "Duration must be at least 1 year")
    @Max(value = 10, message = "Duration must not exceed 10 years")
    private Integer durationYears;

    private Boolean isActive;
}
