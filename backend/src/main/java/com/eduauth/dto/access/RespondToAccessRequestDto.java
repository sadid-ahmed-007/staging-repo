package com.eduauth.dto.access;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body sent by a student when approving or rejecting an access request.
 *
 * POST /api/student/access-requests/{id}/respond
 *
 * If approved == true → durationDays is required.
 * If approved == false → durationDays is ignored.
 *
 * Cross-field validation (durationDays required when approved) is enforced
 * in AccessService.respondToRequest().
 */
@Data
public class RespondToAccessRequestDto {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    private String responseMessage;

    /**
     * Number of days to grant access (student decides — may differ from requested).
     * Required when approved == true; ignored when approved == false.
     */
    @Min(value = 1,   message = "Duration must be at least 1 day")
    @Max(value = 365, message = "Duration cannot exceed 365 days")
    private Integer durationDays;
}
