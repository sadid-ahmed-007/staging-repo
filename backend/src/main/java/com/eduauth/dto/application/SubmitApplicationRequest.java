package com.eduauth.dto.application;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitApplicationRequest {

    @NotNull(message = "University ID is required")
    @JsonAlias("university_id")
    private Long universityId;

    @JsonAlias("certificate_level_id")
    private Long certificateLevelId;

    @JsonAlias("department_id")
    private Long departmentId;

    @JsonAlias("program_id")
    private Long programId;

    @Size(max = 1000, message = "Personal statement must not exceed 1000 characters")
    @JsonAlias("personal_statement")
    private String personalStatement;
}
