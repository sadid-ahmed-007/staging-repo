package com.eduauth.dto.enrollment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WithdrawalRequestDto {

    @NotBlank(message = "Reason is required")
    @Size(min = 20, message = "Reason must be at least 20 characters")
    private String reason;
}
