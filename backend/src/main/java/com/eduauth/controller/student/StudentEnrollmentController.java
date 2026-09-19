package com.eduauth.controller.student;

import com.eduauth.dto.enrollment.EnrollmentResponse;
import com.eduauth.dto.enrollment.WithdrawalRequestDto;
import com.eduauth.model.User;
import com.eduauth.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Student enrollment read/withdrawal endpoints.
 *
 * GET  /api/student/enrollment                          → current enrollment (or null)
 * POST /api/student/enrollment/{id}/request-withdrawal  → request withdrawal
 * GET  /api/student/enrollment/withdrawal-status        → withdrawal request status
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentEnrollmentController {

    private final EnrollmentService enrollmentService;

    // ── GET /api/student/enrollment ───────────────────────────────────────────

    @GetMapping("/api/student/enrollment")
    public ResponseEntity<?> getCurrentEnrollment(
            @AuthenticationPrincipal User user) {

        EnrollmentResponse enrollment = enrollmentService.getStudentCurrentEnrollment(user.getId());

        if (enrollment == null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", (Object) null,
                    "message", "Not currently enrolled"
            ));
        }

        return ResponseEntity.ok(Map.of("success", true, "data", enrollment));
    }

    // ── GET /api/student/enrollment/withdrawal-status ─────────────────────────
    // NOTE: declared before /{id}/... to avoid Spring treating "withdrawal-status" as a path variable

    @GetMapping("/api/student/enrollment/withdrawal-status")
    public ResponseEntity<?> getWithdrawalStatus(
            @AuthenticationPrincipal User user) {

        Map<String, Object> result = enrollmentService.getWithdrawalStatus(user.getId());
        return ResponseEntity.ok(result);
    }

    // ── POST /api/student/enrollment/{id}/request-withdrawal ──────────────────

    @PostMapping("/api/student/enrollment/{id}/request-withdrawal")
    public ResponseEntity<?> requestWithdrawal(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalRequestDto request) {

        EnrollmentResponse response = enrollmentService.requestWithdrawal(id, request, user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Withdrawal request submitted successfully. " +
                           "Please wait for the university to respond.",
                "data", response
        ));
    }

    // ── DELETE /api/student/enrollment/withdrawal-request ─────────────────────

    @RequestMapping(
        value = {
            "/api/student/enrollment/withdrawal-request",
            "/api/student/enrollment/{id}/withdrawal-request",
            "/api/student/enrollment/cancel-withdrawal"
        },
        method = {RequestMethod.DELETE, RequestMethod.POST}
    )
    public ResponseEntity<?> cancelWithdrawal(
            @AuthenticationPrincipal User user,
            @PathVariable(required = false) Long id) {

        enrollmentService.cancelWithdrawalRequest(id, user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Withdrawal request cancelled successfully"
        ));
    }
}
