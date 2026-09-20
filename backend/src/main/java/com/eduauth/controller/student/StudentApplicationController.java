package com.eduauth.controller.student;

import com.eduauth.dto.application.ApplicationResponse;
import com.eduauth.dto.application.SubmitApplicationRequest;
import com.eduauth.model.User;
import com.eduauth.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Student-facing application endpoints.
 *
 * POST   /api/student/applications          → submit application to a university
 * GET    /api/student/applications          → list own applications (paginated)
 * DELETE /api/student/applications/{id}     → cancel pending application
 * GET    /api/student/history               → full timeline (applications + enrollments + certs + access)
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentApplicationController {

    private final ApplicationService applicationService;

    // ── POST /api/student/applications ────────────────────────────────────────

    @PostMapping("/api/student/applications")
    public ResponseEntity<?> submitApplication(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SubmitApplicationRequest request) {

        ApplicationResponse response = applicationService.submitApplication(request, user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Application submitted successfully",
                "data",    response
        ));
    }

    // ── GET /api/student/applications ─────────────────────────────────────────

    @GetMapping("/api/student/applications")
    public ResponseEntity<?> getMyApplications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "15")  int    size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ApplicationResponse> result =
                applicationService.getStudentApplications(user.getId(), status, pageable);

        return ResponseEntity.ok(Map.of(
                "success",      true,
                "data",         result.getContent(),
                "totalPages",   result.getTotalPages(),
                "totalElements",result.getTotalElements(),
                "page",         result.getNumber(),
                "size",         result.getSize()
        ));
    }

    // ── DELETE /api/student/applications/{id} ─────────────────────────────────

    @DeleteMapping("/api/student/applications/{id}")
    public ResponseEntity<?> cancelApplication(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        ApplicationResponse response = applicationService.cancelApplication(id, user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Application cancelled successfully",
                "data",    response
        ));
    }

    // ── GET /api/student/history ───────────────────────────────────────────────

    @GetMapping("/api/student/history")
    public ResponseEntity<?> getStudentHistory(
            @AuthenticationPrincipal User user) {

        java.util.Map<String, Object> history = applicationService.getStudentHistory(user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    history
        ));
    }
}
