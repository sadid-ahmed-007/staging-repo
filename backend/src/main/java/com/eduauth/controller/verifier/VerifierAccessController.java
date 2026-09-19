package com.eduauth.controller.verifier;

import com.eduauth.dto.access.AccessGrantListDto;
import com.eduauth.dto.access.AccessRequestListDto;
import com.eduauth.dto.access.SendAccessRequestDto;
import com.eduauth.exception.BadRequestException;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import com.eduauth.service.AccessService;
import com.eduauth.util.HashUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verifier-side access request and accessible-certificates endpoints.
 *
 * Rate-limiting note: In production, GET /api/verifier/search should be
 * rate-limited to 20 requests/minute per verifier (not implemented here —
 * should be enforced at the API gateway or via a filter/interceptor).
 *
 * Endpoints:
 *   GET    /api/verifier/search
 *   POST   /api/verifier/access-requests
 *   GET    /api/verifier/access-requests
 *   DELETE /api/verifier/access-requests/{id}
 */
@RestController
@RequestMapping("/api/verifier")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VERIFIER')")
public class VerifierAccessController {

    private final VerifierRepository      verifierRepository;
    private final StudentRepository       studentRepository;
    private final UserRepository          userRepository;
    private final UserSettingsRepository  userSettingsRepository;
    private final EnrollmentRepository    enrollmentRepository;
    private final AccessService           accessService;
    private final AccessGrantRepository   accessGrantRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final CertificateRepository   certificateRepository;

    // ── GET /api/verifier/search ──────────────────────────────────────────────

    /**
     * Search for a student by exact identifier.
     *
     * Query params:
     *   type: email | serial | nid | student_id
     *   q:    the search value (exact match — never partial)
     *
     * For NID: q is hashed with SHA-256 before lookup.
     * Always returns { found: true/false } — never leaks partial information.
     *
     * Rate-limiting note: production would enforce 20 requests/minute per verifier.
     */
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<?> searchStudent(
            @AuthenticationPrincipal User user,
            @RequestParam String type,
            @RequestParam String q) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Verifier profile not found"));
        }

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Search query is required"));
        }

        Student student = null;

        Certificate searchedCertificate = null;

        switch (type.toLowerCase()) {
            case "email" -> {
                // Exact email match — role must be student
                student = studentRepository.findByUserEmail(q.trim()).orElse(null);
                // Verify role is student
                if (student != null && (student.getUser() == null
                        || !"student".equals(student.getUser().getRole()))) {
                    student = null;
                }
            }
            case "certificate_serial", "serial", "certificate" -> {
                // Search by Certificate Serial Number
                searchedCertificate = certificateRepository.findBySerial(q.trim()).orElse(null);
                if (searchedCertificate != null) {
                    student = searchedCertificate.getStudent();
                }
            }
            case "student_id" -> {
                // Search by university-assigned roll number (case-insensitive exact match)
                // We look in the enrollments table for a matching roll_number / enrollment_number
                student = findStudentByStudentId(q.trim());
            }
            case "nid" -> {
                // Hash the NID and look up by nid_hash (indexed column — exact O(1) lookup)
                String nidHash = HashUtil.sha256(q.trim());
                student = studentRepository.findByNidHash(nidHash).orElse(null);
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false,
                                "message", "Invalid search type. Use: email, serial, nid, or student_id"));
            }
        }

        // Verify student is approved and not suspended
        if (student == null
                || student.getUser() == null
                || !Boolean.TRUE.equals(student.getUser().getIsApproved())
                || student.getUser().getSuspendedAt() != null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "found", false,
                    "message", "No student found with this identifier"
            ));
        }

        // Check privacy settings
        UserSettings settings = userSettingsRepository
                .findByUserId(student.getUser().getId()).orElse(null);

        boolean allowSearch = settings == null || Boolean.TRUE.equals(settings.getAllowVerifierSearch());
        if (!allowSearch) {
            // Student has opted out — return not found (do not reveal existence)
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "found", false,
                    "message", "No student found with this identifier"
            ));
        }

        boolean showInstitution = settings == null || Boolean.TRUE.equals(settings.getShowInstitutionToPublic());

        // Check access status for this verifier–student pair
        LocalDateTime now = LocalDateTime.now();
        boolean hasActiveAccess;
        boolean hasPendingRequest;

        if (searchedCertificate != null) {
            // Check if verifier has access to this certificate (either all-cert grant or this cert grant)
            hasActiveAccess = !accessGrantRepository.findActiveGrantsForCertificate(
                    verifier.getId(), student.getId(), searchedCertificate.getId(), now).isEmpty();
            hasPendingRequest = accessRequestRepository.existsPendingRequestForCertificate(
                    verifier.getId(), student.getId(), searchedCertificate.getId())
                    || accessRequestRepository.existsPendingRequestForAll(verifier.getId(), student.getId());
        } else {
            hasActiveAccess = accessGrantRepository.existsActiveGrantForAllCertificates(
                    verifier.getId(), student.getId(), now);
            hasPendingRequest = accessRequestRepository.existsPendingRequestForAll(
                    verifier.getId(), student.getId());
        }

        boolean hasActiveAccessToAll = accessGrantRepository.existsActiveGrantForAllCertificates(
                verifier.getId(), student.getId(), now);

        // Build current enrollment info
        Enrollment activeEnrollment = enrollmentRepository
                .findActiveByStudentId(student.getId()).orElse(null);

        Map<String, Object> currentEnrollment = null;
        if (activeEnrollment != null && showInstitution) {
            Institution institution = activeEnrollment.getInstitution();
            currentEnrollment = new LinkedHashMap<>();
            currentEnrollment.put("institutionName",
                    institution != null ? institution.getName() : null);
            currentEnrollment.put("program", activeEnrollment.getProgram());
        }

        String studentIdValue = null;
        if (activeEnrollment != null) {
            studentIdValue = activeEnrollment.getRollNumber() != null
                    ? activeEnrollment.getRollNumber()
                    : activeEnrollment.getEnrollmentNumber();
        } else if ("student_id".equalsIgnoreCase(type)) {
            studentIdValue = q.trim();
        }

        Map<String, Object> studentData = new LinkedHashMap<>();
        studentData.put("id",                   student.getId());
        studentData.put("name",                 (student.getFirstName() + " " + student.getLastName()).trim());
        studentData.put("email",                student.getUser().getEmail()); // Always give email along with searched user
        studentData.put("studentId",            studentIdValue);
        studentData.put("hasActiveAccess",      hasActiveAccess);
        studentData.put("hasActiveAccessToAll", hasActiveAccessToAll);
        studentData.put("hasPendingRequest",    hasPendingRequest);
        studentData.put("currentEnrollment",    currentEnrollment);

        if (searchedCertificate != null) {
            Map<String, Object> certMap = new LinkedHashMap<>();
            certMap.put("id",               searchedCertificate.getId());
            certMap.put("serial",           searchedCertificate.getSerial());
            certMap.put("serialNumber",     searchedCertificate.getSerial());
            certMap.put("certificateName",  searchedCertificate.getCertificateName());
            certMap.put("certificateLevel", searchedCertificate.getCertificateLevel());
            certMap.put("educationLevel",   searchedCertificate.getCertificateLevel());
            certMap.put("issueDate",        searchedCertificate.getIssueDate());
            certMap.put("department",       searchedCertificate.getDepartment());
            certMap.put("major",            searchedCertificate.getMajor());
            studentData.put("searchedCertificate", certMap);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "found",   true,
                "student", studentData
        ));
    }

    // ── POST /api/verifier/access-requests ────────────────────────────────────

    @PostMapping("/access-requests")
    public ResponseEntity<?> sendAccessRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SendAccessRequestDto dto) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Verifier profile not found"));
        }

        AccessRequest req = accessService.sendAccessRequest(dto, verifier.getId());

        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Access request sent successfully",
                "data",    accessService.toRequestDto(req)
        ));
    }

    // ── GET /api/verifier/access-requests ─────────────────────────────────────

    @GetMapping("/access-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyAccessRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "0")   int page,
            @RequestParam(required = false, defaultValue = "10")  int size) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Verifier profile not found"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<AccessRequest> pageResult = accessService.getVerifierRequests(
                verifier.getId(), status, pageable);

        List<AccessRequestListDto> items = pageResult.getContent().stream()
                .map(accessService::toRequestDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    items,
                "total",   pageResult.getTotalElements(),
                "page",    pageResult.getNumber(),
                "pages",   pageResult.getTotalPages()
        ));
    }

    // ── DELETE /api/verifier/access-requests/{id} ─────────────────────────────

    @DeleteMapping("/access-requests/{id}")
    public ResponseEntity<?> cancelAccessRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Verifier profile not found"));
        }

        accessService.cancelAccessRequest(id, verifier.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Access request cancelled successfully"
        ));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Find a student by their university-assigned student/roll number.
     * Searches the enrollments table for a matching roll_number (case-insensitive exact match),
     * then falls back to enrollment_number if not found.
     */
    private Student findStudentByStudentId(String studentIdValue) {
        Optional<Enrollment> enrollment = enrollmentRepository
                .findFirstByRollNumberIgnoreCase(studentIdValue);
        if (enrollment.isEmpty()) {
            enrollment = enrollmentRepository
                    .findFirstByEnrollmentNumberIgnoreCase(studentIdValue);
        }
        return enrollment
                .flatMap(e -> studentRepository.findByIdWithUser(e.getStudentId()))
                .orElse(null);
    }
}
