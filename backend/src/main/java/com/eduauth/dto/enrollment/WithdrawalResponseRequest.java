package com.eduauth.dto.enrollment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WithdrawalResponseRequest {

    private boolean approved;

    @NotBlank(message = "Response message is required")
    @Size(min = 10, message = "Response message must be at least 10 characters")
    private String responseMessage;
}
