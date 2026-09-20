package com.eduauth.controller.university;

import com.eduauth.dto.application.ApplicationResponse;
import com.eduauth.dto.application.ReviewApplicationRequest;
import com.eduauth.model.Institution;
import com.eduauth.model.User;
import com.eduauth.repository.InstitutionRepository;
import com.eduauth.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * University-facing application management endpoints.
 *
 * GET  /api/university/applications                       → list applications (paginated)
 * GET  /api/university/applications/{id}                  → single application with student certs
 * POST /api/university/applications/{id}/review           → accept or reject
 * GET  /api/university/applications/accepted-for-enrollment → accepted applications not yet enrolled
 *                                                            (used for "From Application" tab in enroll modal)
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('UNIVERSITY')")
public class UniversityApplicationController {

    private final ApplicationService    applicationService;
    private final InstitutionRepository institutionRepository;

    // ── GET /api/university/applications ──────────────────────────────────────

    @GetMapping("/api/university/applications")
    public ResponseEntity<?> listApplications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "15")  int    size) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        Pageable pageable = PageRequest.of(page, size);
        Page<ApplicationResponse> result =
                applicationService.getUniversityApplications(institution.getId(), status, pageable);

        return ResponseEntity.ok(Map.of(
                "success",       true,
                "data",          result.getContent(),
                "totalPages",    result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "page",          result.getNumber(),
                "size",          result.getSize()
        ));
    }

    // ── GET /api/university/applications/accepted-for-enrollment ─────────────
    // NOTE: Must be declared BEFORE /{id} to avoid Spring interpreting
    //       "accepted-for-enrollment" as a path variable.

    @GetMapping("/api/university/applications/accepted-for-enrollment")
    public ResponseEntity<?> getAcceptedForEnrollment(
            @AuthenticationPrincipal User user) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        List<ApplicationResponse> applications =
                applicationService.getAcceptedNotYetEnrolled(institution.getId());

        // Augment each with an enrollmentHint for the modal pre-fill
        List<Map<String, Object>> result = applications.stream()
                .map(app -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",                   app.getId());
                    m.put("studentId",             app.getStudentId());
                    m.put("studentName",           app.getStudentName());
                    m.put("studentEmail",          app.getStudentEmail());
                    m.put("universityId",          app.getUniversityId());
                    m.put("universityName",        app.getUniversityName());
                    m.put("certificateLevelId",    app.getCertificateLevelId());
                    m.put("certificateLevelName",  app.getCertificateLevelName());
                    m.put("departmentId",          app.getDepartmentId());
                    m.put("departmentName",        app.getDepartmentName());
                    m.put("programId",             app.getProgramId());
                    m.put("programName",           app.getProgramName());
                    m.put("status",                app.getStatus());
                    m.put("appliedAt",             app.getAppliedAt());
                    m.put("acceptanceMessage",     app.getAcceptanceMessage());

                    // Enrollment hint for pre-filling the enrollment form
                    Map<String, Object> hint = new LinkedHashMap<>();
                    hint.put("studentId",          app.getStudentId());
                    hint.put("programId",          app.getProgramId());
                    hint.put("departmentId",       app.getDepartmentId());
                    hint.put("certificateLevelId", app.getCertificateLevelId());
                    m.put("enrollmentHint", hint);

                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    result
        ));
    }

    // ── GET /api/university/applications/{id} ─────────────────────────────────

    @GetMapping("/api/university/applications/{id}")
    public ResponseEntity<?> getApplication(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        ApplicationResponse application =
                applicationService.getApplicationDetail(id, institution.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    application
        ));
    }

    // ── POST /api/university/applications/{id}/review ─────────────────────────

    @PostMapping("/api/university/applications/{id}/review")
    public ResponseEntity<?> reviewApplication(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ReviewApplicationRequest request) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        ApplicationResponse application =
                applicationService.reviewApplication(id, request, institution.getId(), user.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", Boolean.TRUE.equals(request.getApproved())
                ? "Application accepted. The student has been notified."
                : "Application rejected. The student has been notified.");
        response.put("data", application);

        // If accepted, include enrollment hint for the frontend to pre-fill the enroll modal
        if (Boolean.TRUE.equals(request.getApproved())) {
            Map<String, Object> enrollmentHint = new LinkedHashMap<>();
            enrollmentHint.put("studentId",          application.getStudentId());
            enrollmentHint.put("programId",          application.getProgramId());
            enrollmentHint.put("departmentId",       application.getDepartmentId());
            enrollmentHint.put("certificateLevelId", application.getCertificateLevelId());
            response.put("enrollmentHint", enrollmentHint);
        }

        return ResponseEntity.ok(response);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Institution resolveInstitution(User user) {
        return institutionRepository.findByUserId(user.getId()).orElse(null);
    }

    private ResponseEntity<?> institutionNotFound() {
        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Institution profile not found for this account"
        ));
    }
}
