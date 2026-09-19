package com.eduauth.controller.university;

import com.eduauth.dto.enrollment.*;
import com.eduauth.model.Institution;
import com.eduauth.model.User;
import com.eduauth.repository.InstitutionRepository;
import com.eduauth.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * University enrollment management endpoints.
 *
 * POST   /api/university/enrollments                         → enroll student
 * GET    /api/university/enrollments                         → list enrollments (paginated)
 * GET    /api/university/enrollments/withdrawal-requests     → pending withdrawal requests
 * GET    /api/university/enrollments/{id}                    → get enrollment details
 * PUT    /api/university/enrollments/{id}                    → edit enrollment
 * PATCH  /api/university/enrollments/{id}/extend-graduation  → extend graduation date
 * POST   /api/university/enrollments/{id}/withdraw           → direct withdraw student
 * POST   /api/university/enrollments/{id}/respond-withdrawal → approve/reject withdrawal
 * GET    /api/university/students/search-to-enroll           → search students to enroll
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('UNIVERSITY')")
public class UniversityEnrollmentController {

    private final EnrollmentService enrollmentService;
    private final InstitutionRepository institutionRepository;

    // ── POST /api/university/enrollments ──────────────────────────────────────

    @PostMapping("/api/university/enrollments")
    public ResponseEntity<?> enrollStudent(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody EnrollRequest request) {

        Institution institution = resolveInstitution(user);
        if (institution == null) {
            return institutionNotFound();
        }

        EnrollmentResponse response = enrollmentService.enrollStudent(
                request, institution.getId(), user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Student enrolled successfully",
                "data", response
        ));
    }

    // ── GET /api/university/enrollments ───────────────────────────────────────

    @GetMapping("/api/university/enrollments")
    public ResponseEntity<?> listEnrollments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        Pageable pageable = PageRequest.of(page, size);
        Page<EnrollmentResponse> result = enrollmentService.getEnrollments(
                institution.getId(), status, search, pageable);

        // Stats for this institution
        long totalActive       = enrollmentService.countByStatus(institution.getId(), "active");
        long totalGraduated    = enrollmentService.countByStatus(institution.getId(), "graduated");
        long totalWithdrawn    = enrollmentService.countByStatus(institution.getId(), "withdrawn");
        long totalAll          = enrollmentService.countAll(institution.getId());
        int pendingWithdrawals = enrollmentService.getPendingWithdrawalRequests(institution.getId()).size();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "enrollments", result.getContent(),
                "stats", Map.of(
                        "total",              totalAll,
                        "active",             totalActive,
                        "graduated",          totalGraduated,
                        "withdrawn",          totalWithdrawn,
                        "pendingWithdrawals", pendingWithdrawals
                ),
                "pagination", Map.of(
                        "currentPage", result.getNumber(),
                        "totalPages",  result.getTotalPages(),
                        "totalItems",  result.getTotalElements(),
                        "perPage",     result.getSize()
                )
        ));
    }

    // ── GET /api/university/enrollments/withdrawal-requests ───────────────────
    // NOTE: must be declared BEFORE /{id} to avoid ambiguity

    @GetMapping({"/api/university/enrollments/withdrawal-requests", "/api/university/withdrawal/pending"})
    public ResponseEntity<?> listWithdrawalRequests(
            @AuthenticationPrincipal User user) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        List<EnrollmentResponse> pending =
                enrollmentService.getPendingWithdrawalRequests(institution.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", pending,
                "requests", pending,
                "count", pending.size()
        ));
    }

    // ── GET /api/university/enrollments/{id} ──────────────────────────────────

    @GetMapping("/api/university/enrollments/{id}")
    public ResponseEntity<?> getEnrollment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        EnrollmentResponse response = enrollmentService.getEnrollmentById(id, institution.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    // ── PUT /api/university/enrollments/{id} ──────────────────────────────────

    @PutMapping("/api/university/enrollments/{id}")
    public ResponseEntity<?> editEnrollment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody EditEnrollmentRequest request) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        EnrollmentResponse response = enrollmentService.editEnrollment(
                id, request, institution.getId(), user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Enrollment updated successfully",
                "data", response
        ));
    }

    // ── PATCH /api/university/enrollments/{id}/extend-graduation ──────────────

    @PatchMapping("/api/university/enrollments/{id}/extend-graduation")
    public ResponseEntity<?> extendGraduation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ExtendGraduationRequest request) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        EnrollmentResponse response = enrollmentService.extendGraduation(
                id, request, institution.getId(), user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Graduation date extended successfully",
                "data", response
        ));
    }

    // ── POST /api/university/enrollments/{id}/withdraw ────────────────────────

    @PostMapping("/api/university/enrollments/{id}/withdraw")
    public ResponseEntity<?> directWithdraw(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        String reason = (body != null && body.containsKey("reason"))
                ? body.get("reason") : "Withdrawn by university";

        EnrollmentResponse response = enrollmentService.directWithdraw(
                id, reason, institution.getId(), user.getId());

        Map<String, Object> respMap = new java.util.LinkedHashMap<>();
        respMap.put("success", true);
        respMap.put("message", "Student withdrawn successfully");
        respMap.put("data", response);
        return ResponseEntity.ok(respMap);
    }

    // ── POST /api/university/enrollments/{id}/respond-withdrawal ──────────────

    @PostMapping("/api/university/enrollments/{id}/respond-withdrawal")
    public ResponseEntity<?> respondToWithdrawal(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalResponseRequest request) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        EnrollmentResponse response = enrollmentService.respondToWithdrawal(
                id, request, institution.getId(), user.getId());

        String msg = request.isApproved()
                ? "Withdrawal request approved — student is now withdrawn."
                : "Withdrawal request rejected — enrollment remains active.";

        Map<String, Object> respMap = new java.util.LinkedHashMap<>();
        respMap.put("success", true);
        respMap.put("message", msg);
        respMap.put("data", response);
        return ResponseEntity.ok(respMap);
    }

    // ── GET /api/university/students/search-to-enroll ─────────────────────────

    @GetMapping("/api/university/students/search-to-enroll")
    public ResponseEntity<?> searchStudentsToEnroll(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "q", required = false, defaultValue = "") String q) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        if (q.trim().length() < 2) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Search query must be at least 2 characters"
            ));
        }

        List<Map<String, Object>> students =
                enrollmentService.searchStudentsToEnroll(q, institution.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "students", students
        ));
    }

    // ── GET /api/university/students/search ───────────────────────────────────

    @GetMapping("/api/university/students/search")
    public ResponseEntity<?> searchEnrolledStudents(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "q", required = false, defaultValue = "") String q) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        String query = (q != null) ? q.trim() : "";
        List<Map<String, Object>> students =
                enrollmentService.searchEnrolledStudents(query, institution.getId());

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("success", true);
        resp.put("students", students);
        return ResponseEntity.ok(resp);
    }

    // ── GET /api/university/profile ───────────────────────────────────────────

    @GetMapping({"/api/university/profile"})
    public ResponseEntity<?> getUniversityProfile(@AuthenticationPrincipal User user) {
        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        String authName = institution.getDefaultAuthorityName() != null ? institution.getDefaultAuthorityName() : "";
        String authTitle = institution.getDefaultAuthorityTitle() != null ? institution.getDefaultAuthorityTitle() : "";

        Map<String, Object> profileMap = new java.util.LinkedHashMap<>();
        profileMap.put("id", institution.getId());
        profileMap.put("name", institution.getName());
        profileMap.put("registrationNumber", institution.getRegistrationNumber());
        profileMap.put("registration_number", institution.getRegistrationNumber());
        profileMap.put("address", institution.getAddress());
        profileMap.put("city", institution.getCity());
        profileMap.put("phone", institution.getPhone());
        profileMap.put("website", institution.getWebsite());
        profileMap.put("defaultAuthorityName", authName);
        profileMap.put("default_authority_name", authName);
        profileMap.put("defaultAuthorityTitle", authTitle);
        profileMap.put("default_authority_title", authTitle);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "profile", profileMap,
                "defaultAuthorityName", authName,
                "defaultAuthorityTitle", authTitle,
                "default_authority_name", authName,
                "default_authority_title", authTitle
        ));
    }

    // ── PUT /api/university/profile ───────────────────────────────────────────

    @PutMapping({"/api/university/profile"})
    @Transactional
    public ResponseEntity<?> updateUniversityProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        Institution institution = resolveInstitution(user);
        if (institution == null) return institutionNotFound();

        if (body.containsKey("defaultAuthorityName")) {
            institution.setDefaultAuthorityName((String) body.get("defaultAuthorityName"));
        } else if (body.containsKey("default_authority_name")) {
            institution.setDefaultAuthorityName((String) body.get("default_authority_name"));
        }

        if (body.containsKey("defaultAuthorityTitle")) {
            institution.setDefaultAuthorityTitle((String) body.get("defaultAuthorityTitle"));
        } else if (body.containsKey("default_authority_title")) {
            institution.setDefaultAuthorityTitle((String) body.get("default_authority_title"));
        }

        if (body.containsKey("phone") && body.get("phone") != null) {
            institution.setPhone((String) body.get("phone"));
        }
        if (body.containsKey("website") && body.get("website") != null) {
            institution.setWebsite((String) body.get("website"));
        }
        if (body.containsKey("address") && body.get("address") != null) {
            institution.setAddress((String) body.get("address"));
        }
        if (body.containsKey("city") && body.get("city") != null) {
            institution.setCity((String) body.get("city"));
        }

        institution = institutionRepository.save(institution);

        String authName = institution.getDefaultAuthorityName() != null ? institution.getDefaultAuthorityName() : "";
        String authTitle = institution.getDefaultAuthorityTitle() != null ? institution.getDefaultAuthorityTitle() : "";

        Map<String, Object> profileMap = new java.util.LinkedHashMap<>();
        profileMap.put("id", institution.getId());
        profileMap.put("name", institution.getName());
        profileMap.put("registrationNumber", institution.getRegistrationNumber());
        profileMap.put("registration_number", institution.getRegistrationNumber());
        profileMap.put("address", institution.getAddress());
        profileMap.put("city", institution.getCity());
        profileMap.put("phone", institution.getPhone());
        profileMap.put("website", institution.getWebsite());
        profileMap.put("defaultAuthorityName", authName);
        profileMap.put("default_authority_name", authName);
        profileMap.put("defaultAuthorityTitle", authTitle);
        profileMap.put("default_authority_title", authTitle);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile updated successfully",
                "profile", profileMap,
                "defaultAuthorityName", authName,
                "defaultAuthorityTitle", authTitle,
                "default_authority_name", authName,
                "default_authority_title", authTitle
        ));
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
