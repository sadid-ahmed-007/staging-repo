package com.eduauth.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewApplicationRequest {

    /**
     * true  → accept the application (message becomes acceptance_message)
     * false → reject the application (message becomes rejection_reason)
     */
    @NotNull(message = "Decision (approved) is required")
    private Boolean approved;

    /**
     * Acceptance message or rejection reason depending on the decision.
     * Required, min 10 characters.
     */
    @NotBlank(message = "Message is required")
    @Size(min = 10, message = "Message must be at least 10 characters")
    private String message;
}
