package com.eduauth.controller.admin;

import com.eduauth.model.ActivityLog;
import com.eduauth.model.Certificate;
import com.eduauth.model.User;
import com.eduauth.repository.ActivityLogRepository;
import com.eduauth.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCertificateController {

        private final CertificateRepository certificateRepository;
        private final ActivityLogRepository activityLogRepository;

        // ── GET /api/admin/certificates ───────────────────────────────────────────

        @GetMapping
        public ResponseEntity<?> listCertificates(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "25") int size,
                        @RequestParam(defaultValue = "all") String status,
                        @RequestParam(required = false) Long institutionId,
                        @RequestParam(required = false) String search) {

                Pageable pageable = PageRequest.of(page, size);
                Page<Certificate> certPage = certificateRepository.findAllWithFilters(
                                status,
                                institutionId,
                                (search != null && !search.isBlank()) ? search : null,
                                pageable);

                List<Map<String, Object>> items = certPage.getContent().stream()
                                .map(c -> {
                                        Map<String, Object> m = new LinkedHashMap<>();
                                        m.put("id", c.getId());
                                        m.put("serial", c.getSerial());
                                        m.put("studentName", c.getStudentDisplayName());
                                        m.put("institutionName",
                                                        c.getInstitution() != null ? c.getInstitution().getName()
                                                                        : null);
                                        m.put("certificateLevel", c.getCertificateLevel());
                                        m.put("issueDate", c.getIssueDate());
                                        m.put("status", c.isRevoked() ? "revoked" : "active");
                                        m.put("revokedAt", c.getRevokedAt());
                                        return m;
                                })
                                .collect(Collectors.toList());

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "data", items,
                                "total", certPage.getTotalElements(),
                                "page", certPage.getNumber(),
                                "pages", certPage.getTotalPages()));
        }

        // ── GET /api/admin/certificates/{id} ─────────────────────────────────────

        @GetMapping("/{id}")
        @Transactional(readOnly = true)
        public ResponseEntity<?> getCertificate(@PathVariable Long id) {

                Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
                if (cert == null) {
                        return ResponseEntity.status(404)
                                        .body(Map.of("success", false, "message", "Certificate not found"));
                }

                Map<String, Object> studentInfo = new LinkedHashMap<>();
                if (cert.getStudent() != null) {
                        studentInfo.put("id", cert.getStudent().getId());
                        studentInfo.put("firstName", cert.getStudent().getFirstName());
                        studentInfo.put("lastName", cert.getStudent().getLastName());
                        studentInfo.put("email", cert.getStudent().getUser() != null
                                        ? cert.getStudent().getUser().getEmail()
                                        : null);
                        studentInfo.put("dateOfBirth", cert.getStudent().getDateOfBirth());
                }

                Map<String, Object> institutionInfo = new LinkedHashMap<>();
                if (cert.getInstitution() != null) {
                        institutionInfo.put("id", cert.getInstitution().getId());
                        institutionInfo.put("name", cert.getInstitution().getName());
                        institutionInfo.put("registrationNumber", cert.getInstitution().getRegistrationNumber());
                }

                Map<String, Object> enrollmentInfo = null;
                if (cert.getEnrollment() != null) {
                        enrollmentInfo = new LinkedHashMap<>();
                        enrollmentInfo.put("enrollmentNumber", cert.getEnrollment().getEnrollmentNumber());
                        enrollmentInfo.put("rollNumber", cert.getEnrollment().getRollNumber());
                        enrollmentInfo.put("program", cert.getEnrollment().getProgram());
                        enrollmentInfo.put("batch", cert.getEnrollment().getBatch());
                        enrollmentInfo.put("status", cert.getEnrollment().getStatus());
                }

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", cert.getId());
                data.put("serial", cert.getSerial());
                data.put("issuedName", cert.getIssuedName());
                data.put("certificateLevel", cert.getCertificateLevel());
                data.put("certificateName", cert.getCertificateName());
                data.put("department", cert.getDepartment());
                data.put("major", cert.getMajor());
                data.put("session", cert.getSession());
                data.put("cgpa", cert.getCgpa());
                data.put("degreeClass", cert.getDegreeClass());
                data.put("issueDate", cert.getIssueDate());
                data.put("convocationDate", cert.getConvocationDate());
                data.put("authorityName", cert.getAuthorityName());
                data.put("authorityTitle", cert.getAuthorityTitle());
                data.put("isPubliclyShareable", cert.getIsPubliclyShareable());
                data.put("status", cert.isRevoked() ? "revoked" : "active");
                data.put("revokedAt", cert.getRevokedAt());
                data.put("revokedByRole", cert.getRevokedByRole());
                data.put("revokedById", cert.getRevokedById());
                data.put("revocationReason", cert.getRevocationReason());
                data.put("revocationHistory", cert.getRevocationHistory());
                data.put("createdAt", cert.getCreatedAt());
                data.put("updatedAt", cert.getUpdatedAt());
                data.put("student", studentInfo);
                data.put("institution", institutionInfo);
                data.put("enrollment", enrollmentInfo);

                return ResponseEntity.ok(Map.of("success", true, "data", data));
        }

        // ── POST /api/admin/certificates/{id}/revoke ──────────────────────────────

        @PostMapping("/{id}/revoke")
        public ResponseEntity<?> revokeCertificate(
                        @AuthenticationPrincipal User user,
                        @PathVariable Long id,
                        @RequestBody Map<String, Object> body) {

                Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
                if (cert == null) {
                        return ResponseEntity.status(404)
                                        .body(Map.of("success", false, "message", "Certificate not found"));
                }
                if (cert.isRevoked()) {
                        return ResponseEntity.status(422)
                                        .body(Map.of("success", false, "message", "Certificate is already revoked"));
                }

                String reason = body.get("reason") != null ? body.get("reason").toString().trim() : "";
                if (reason.length() < 10) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "errors", Map.of("reason", "Reason must be at least 10 characters")));
                }

                cert.setRevokedAt(LocalDateTime.now());
                cert.setRevokedById(user.getId());
                cert.setRevokedByRole("admin");
                cert.setRevocationReason(reason);
                certificateRepository.save(cert);

                // Log activity
                ActivityLog log = new ActivityLog();
                log.setUserId(user.getId());
                log.setAction("CERTIFICATE_REVOKED");
                log.setEntityType("Certificate");
                log.setEntityId(cert.getId());
                log.setDescription("Certificate " + cert.getSerial() + " revoked by admin. Reason: " + reason);
                activityLogRepository.save(log);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Certificate revoked successfully",
                                "data", Map.of(
                                                "id", cert.getId(),
                                                "serial", cert.getSerial(),
                                                "status", "revoked",
                                                "revokedAt", cert.getRevokedAt(),
                                                "revocationReason", cert.getRevocationReason())));
        }

        // ── POST /api/admin/certificates/{id}/restore ─────────────────────────────

        @PostMapping("/{id}/restore")
        public ResponseEntity<?> restoreCertificate(
                        @AuthenticationPrincipal User user,
                        @PathVariable Long id,
                        @RequestBody(required = false) Map<String, Object> body) {

                Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
                if (cert == null) {
                        return ResponseEntity.status(404)
                                        .body(Map.of("success", false, "message", "Certificate not found"));
                }
                if (!cert.isRevoked()) {
                        return ResponseEntity.status(422)
                                        .body(Map.of("success", false, "message", "Certificate is not revoked"));
                }

                // Optional reason for audit
                String reason = (body != null && body.get("reason") != null)
                                ? body.get("reason").toString().trim()
                                : "Restored by admin";

                cert.setRevokedAt(null);
                cert.setRevokedById(null);
                cert.setRevokedByRole(null);
                cert.setRevocationReason(null);
                certificateRepository.save(cert);

                // Log activity
                ActivityLog log = new ActivityLog();
                log.setUserId(user.getId());
                log.setAction("CERTIFICATE_RESTORED");
                log.setEntityType("Certificate");
                log.setEntityId(cert.getId());
                log.setDescription("Certificate " + cert.getSerial() + " restored by admin. Reason: " + reason);
                activityLogRepository.save(log);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Certificate restored successfully",
                                "data", Map.of(
                                                "id", cert.getId(),
                                                "serial", cert.getSerial(),
                                                "status", "active")));
        }
}
