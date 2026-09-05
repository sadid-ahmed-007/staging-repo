package com.eduauth.controller.verifier;

import com.eduauth.model.AccessGrant;
import com.eduauth.model.Certificate;
import com.eduauth.model.Student;
import com.eduauth.model.User;
import com.eduauth.model.Verifier;
import com.eduauth.repository.AccessGrantRepository;
import com.eduauth.repository.CertificateRepository;
import com.eduauth.repository.StudentRepository;
import com.eduauth.repository.VerifierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verifier endpoints for accessible certificates.
 *
 * GET /api/verifier/accessible-certificates                      → list of students this verifier has access to
 * GET /api/verifier/accessible-certificates/{studentId}          → all certs for a specific student
 */
@RestController
@RequestMapping("/api/verifier/accessible-certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VERIFIER')")
public class VerifierCertificateController {

    private final VerifierRepository     verifierRepository;
    private final AccessGrantRepository  accessGrantRepository;
    private final StudentRepository      studentRepository;
    private final CertificateRepository  certificateRepository;

    // ── GET /api/verifier/accessible-certificates ─────────────────────────────

    @GetMapping
    public ResponseEntity<?> listAccessibleStudents(@AuthenticationPrincipal User user) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Verifier profile not found"));
        }

        LocalDateTime now = LocalDateTime.now();
        List<AccessGrant> grants = accessGrantRepository.findActiveGrantsForVerifier(verifier.getId(), now);

        List<Map<String, Object>> students = new ArrayList<>();
        for (AccessGrant grant : grants) {
            Student student = studentRepository.findById(grant.getStudentId()).orElse(null);
            if (student == null) continue;

            long certCount = certificateRepository.countByStudentId(student.getId());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId",           student.getId());
            item.put("studentName",         buildFullName(student));
            item.put("studentEmail",        student.getUser() != null ? student.getUser().getEmail() : null);
            item.put("accessGrantedAt",     grant.getExpiresAt() != null
                                                ? grant.getExpiresAt().minusDays(30) : null); // approximation
            item.put("accessExpiresAt",     grant.getExpiresAt());
            item.put("certificateCount",    certCount);
            students.add(item);
        }

        return ResponseEntity.ok(Map.of("success", true, "data", students));
    }

    // ── GET /api/verifier/accessible-certificates/{studentId} ────────────────

    @GetMapping("/{studentId}")
    public ResponseEntity<?> getStudentCertificates(
            @AuthenticationPrincipal User user,
            @PathVariable Long studentId) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Verifier profile not found"));
        }

        // ── Enforce access grant check ────────────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        AccessGrant grant = accessGrantRepository
                .findActiveGrant(verifier.getId(), studentId, now)
                .orElse(null);

        if (grant == null) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false,
                                 "message", "Access to this student has expired or been revoked"));
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student not found"));
        }

        // Return non-revoked certs only
        List<Certificate> certs = certificateRepository
                .findByStudentIdAndRevokedAtIsNullOrderByIssueDateDesc(studentId);

        List<Map<String, Object>> items = certs.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",                   c.getId());
                    m.put("serial",               c.getSerial());
                    m.put("certificateName",       c.getCertificateName());
                    m.put("certificateLevel",      c.getCertificateLevel());
                    m.put("institutionName",       c.getInstitution() != null ? c.getInstitution().getName() : null);
                    m.put("department",            c.getDepartment());
                    m.put("major",                 c.getMajor());
                    m.put("cgpa",                  c.getCgpa());
                    m.put("degreeClass",           c.getDegreeClass());
                    m.put("issueDate",             c.getIssueDate());
                    m.put("status",                "active");
                    m.put("isPubliclyShareable",   c.getIsPubliclyShareable());
                    m.put("createdAt",             c.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> studentInfo = new LinkedHashMap<>();
        studentInfo.put("id",        student.getId());
        studentInfo.put("name",      buildFullName(student));
        studentInfo.put("email",     student.getUser() != null ? student.getUser().getEmail() : null);

        return ResponseEntity.ok(Map.of(
                "success",      true,
                "student",      studentInfo,
                "accessExpires", grant.getExpiresAt(),
                "data",         items,
                "total",        items.size()
        ));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String buildFullName(Student s) {
        if (s.getFirstName() == null && s.getLastName() == null) {
            return s.getUser() != null ? s.getUser().getEmail() : "N/A";
        }
        String name = (s.getFirstName() != null ? s.getFirstName() : "")
                + (s.getMiddleName() != null ? " " + s.getMiddleName() : "")
                + (s.getLastName()  != null ? " " + s.getLastName() : "");
        return name.trim();
    }
}
