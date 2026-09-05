package com.eduauth.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ResendOtpRequest {

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;
}
