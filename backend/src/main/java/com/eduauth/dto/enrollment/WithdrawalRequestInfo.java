package com.eduauth.dto.enrollment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Embedded in EnrollmentResponse when a withdrawal request exists for the enrollment.
 * Carries all the information the university needs to understand the withdrawal history.
 */
@Data
@Builder
public class WithdrawalRequestInfo {

    /**
     * Who initiated the withdrawal.
     * "student" — the student filed a withdrawal request.
     * "university" — the university directly withdrew the student (no WR record exists;
     *                this value is synthesised by the service when there is no WR row).
     */
    private String requestedBy;

    /** The reason text provided by whoever initiated the withdrawal. */
    private String reason;

    /** When the withdrawal request / direct-withdrawal occurred. */
    private LocalDateTime requestedAt;

    /**
     * University's response message (maps to rejection_note, used for both approved/rejected).
     * null for direct-university withdrawals (there is no approval step).
     */
    private String responseMessage;

    /** When the university responded (reviewed_at). null if still pending or direct-withdrawal. */
    private LocalDateTime respondedAt;

    /**
     * "pending", "approved", "rejected", or "direct" (for university direct-withdrawal).
     */
    private String status;
}
