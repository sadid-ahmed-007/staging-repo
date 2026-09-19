package com.eduauth.controller.student;

import com.eduauth.dto.access.AccessGrantListDto;
import com.eduauth.dto.access.AccessRequestListDto;
import com.eduauth.dto.access.RespondToAccessRequestDto;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import com.eduauth.service.AccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Student-side access request and grant endpoints.
 *
 * All endpoints here perform a critical security check:
 * the logged-in student's ID must match the request/grant's student_id.
 * This is enforced both by the repository queries and in AccessService.
 *
 * Endpoints:
 *   GET    /api/student/access-requests
 *   POST   /api/student/access-requests/{id}/respond
 *   GET    /api/student/access-grants
 *   DELETE /api/student/access-grants/{id}
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentAccessController {

    private final StudentRepository studentRepository;
    private final AccessService     accessService;

    // ── GET /api/student/access-requests ──────────────────────────────────────

    /**
     * Returns paginated list of access requests received by the logged-in student.
     *
     * Query params:
     *   status: pending | approved | rejected | all (default: all)
     *   page:   0-indexed page number (default: 0)
     *   size:   page size (default: 10)
     *
     * Security: only returns requests where student_id matches the logged-in student.
     */
    @GetMapping("/access-requests")
    public ResponseEntity<?> getMyAccessRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "0")   int page,
            @RequestParam(required = false, defaultValue = "10")  int size) {

        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        Pageable pageable = PageRequest.of(page, size);
        // Security: getStudentRequests always filters by studentId
        Page<AccessRequest> pageResult = accessService.getStudentRequests(
                student.getId(), status, pageable);

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

    // ── POST /api/student/access-requests/{id}/respond ────────────────────────

    /**
     * Student approves or rejects a pending access request.
     *
     * Security: respondToRequest verifies the request belongs to this student
     * (student_id in the DB row must match logged-in student's ID).
     */
    @PostMapping("/access-requests/{id}/respond")
    public ResponseEntity<?> respondToRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RespondToAccessRequestDto dto) {

        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        // Security: pass student.getId() so service can verify ownership
        AccessRequest updated = accessService.respondToRequest(id, dto, student.getId());

        String action = Boolean.TRUE.equals(dto.getApproved()) ? "approved" : "rejected";
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Access request " + action + " successfully",
                "data",    accessService.toRequestDto(updated)
        ));
    }

    // ── GET /api/student/access-grants ────────────────────────────────────────

    /**
     * Returns all active grants (verifiers currently with access to this student's certs)
     * plus historical grants (expired or revoked) for audit purposes.
     *
     * Response shape:
     * {
     *   success: true,
     *   active:  [ AccessGrantListDto... ],
     *   history: [ AccessGrantListDto... ]
     * }
     */
    @GetMapping("/access-grants")
    public ResponseEntity<?> getMyGrants(@AuthenticationPrincipal User user) {

        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        Map<String, Object> grants = accessService.getStudentGrants(student.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "active",  grants.get("active"),
                "history", grants.get("history")
        ));
    }

    // ── DELETE /api/student/access-grants/{id} ────────────────────────────────

    /**
     * Student revokes an active access grant.
     *
     * Security: revokeGrant verifies the grant's student_id matches the logged-in student.
     * Will throw 403 if the grant belongs to a different student.
     */
    @DeleteMapping("/access-grants/{id}")
    public ResponseEntity<?> revokeGrant(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        // Security: student.getId() is passed — service verifies ownership
        accessService.revokeGrant(id, student.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Access revoked successfully"
        ));
    }
}
