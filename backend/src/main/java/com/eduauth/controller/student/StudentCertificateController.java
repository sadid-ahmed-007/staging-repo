package com.eduauth.controller.student;

import com.eduauth.model.Certificate;
import com.eduauth.model.Student;
import com.eduauth.model.User;
import com.eduauth.repository.CertificateRepository;
import com.eduauth.repository.StudentRepository;
import com.eduauth.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Student certificate endpoints.
 *
 * GET  /api/student/certificates                     → paginated list (filter: all/public/private)
 * GET  /api/student/certificates/{id}               → full details + share link
 * GET  /api/student/certificates/{id}/pdf           → PDF download with QR code
 * PATCH /api/student/certificates/{id}/visibility   → toggle isPubliclyShareable
 */
@RestController
@RequestMapping("/api/student/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentCertificateController {

    private final StudentRepository       studentRepository;
    private final CertificateRepository   certificateRepository;
    private final CertificateService      certificateService;

    // ── GET /api/student/certificates ────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> listCertificates(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "all")  String filter,
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "25")   int    size) {

        Student student = studentRepository.findByUserId(user.getId())
                .orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Certificate> certPage = certificateRepository
                .findByStudentIdWithFilter(student.getId(), filter, pageable);

        List<Map<String, Object>> items = certPage.getContent().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",                   c.getId());
                    m.put("serial",               c.getSerial());
                    m.put("certificateName",       c.getCertificateName());
                    m.put("certificateLevel",      c.getCertificateLevel());
                    m.put("institutionName",       c.getInstitution() != null ? c.getInstitution().getName() : null);
                    m.put("cgpa",                  c.getCgpa());
                    m.put("degreeClass",           c.getDegreeClass());
                    m.put("issueDate",             c.getIssueDate());
                    m.put("status",                c.isRevoked() ? "revoked" : "active");
                    m.put("isPubliclyShareable",   c.getIsPubliclyShareable());
                    m.put("createdAt",             c.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    items,
                "total",   certPage.getTotalElements(),
                "page",    certPage.getNumber(),
                "pages",   certPage.getTotalPages()
        ));
    }

    // ── GET /api/student/certificates/{id} ───────────────────────────────────

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCertificate(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
        if (cert == null || !cert.getStudentId().equals(student.getId())) {
            return ResponseEntity.status(cert == null ? 404 : 403)
                    .body(Map.of("success", false,
                                 "message", cert == null ? "Certificate not found"
                                                         : "Access denied"));
        }

        String shareLink = certificateService.buildShareLink(cert);

        // Student info
        Map<String, Object> studentInfo = new LinkedHashMap<>();
        studentInfo.put("id",        student.getId());
        studentInfo.put("firstName",  student.getFirstName());
        studentInfo.put("lastName",   student.getLastName());
        studentInfo.put("email",      user.getEmail());

        // Institution info
        Map<String, Object> institutionInfo = null;
        if (cert.getInstitution() != null) {
            institutionInfo = new LinkedHashMap<>();
            institutionInfo.put("id",   cert.getInstitution().getId());
            institutionInfo.put("name", cert.getInstitution().getName());
        }

        // Enrollment info
        Map<String, Object> enrollmentInfo = null;
        if (cert.getEnrollment() != null) {
            enrollmentInfo = new LinkedHashMap<>();
            enrollmentInfo.put("enrollmentNumber", cert.getEnrollment().getEnrollmentNumber());
            enrollmentInfo.put("rollNumber",       cert.getEnrollment().getRollNumber());
            enrollmentInfo.put("program",          cert.getEnrollment().getProgram());
            enrollmentInfo.put("batch",            cert.getEnrollment().getBatch());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id",                  cert.getId());
        data.put("serial",              cert.getSerial());
        data.put("certificateLevel",    cert.getCertificateLevel());
        data.put("certificateName",     cert.getCertificateName());
        data.put("department",          cert.getDepartment());
        data.put("major",               cert.getMajor());
        data.put("session",             cert.getSession());
        data.put("cgpa",                cert.getCgpa());
        data.put("degreeClass",         cert.getDegreeClass());
        data.put("issueDate",           cert.getIssueDate());
        data.put("convocationDate",     cert.getConvocationDate());
        data.put("authorityName",       cert.getAuthorityName());
        data.put("authorityTitle",      cert.getAuthorityTitle());
        data.put("status",              cert.isRevoked() ? "revoked" : "active");
        data.put("isPubliclyShareable", cert.getIsPubliclyShareable());
        data.put("revokedAt",           cert.getRevokedAt());
        data.put("revocationReason",    cert.getRevocationReason());
        data.put("createdAt",           cert.getCreatedAt());
        data.put("shareLink",           shareLink);
        data.put("student",             studentInfo);
        data.put("institution",         institutionInfo);
        data.put("enrollment",          enrollmentInfo);

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // ── GET /api/student/certificates/{id}/pdf ───────────────────────────────

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> downloadPdf(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
        if (cert == null || !cert.getStudentId().equals(student.getId())) {
            return ResponseEntity.status(cert == null ? 404 : 403)
                    .body(Map.of("success", false,
                                 "message", cert == null ? "Certificate not found"
                                                         : "Access denied"));
        }

        return certificateService.generatePdf(cert);
    }

    // ── PATCH /api/student/certificates/{id}/visibility ──────────────────────

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<?> updateVisibility(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student profile not found"));
        }

        Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
        if (cert == null || !cert.getStudentId().equals(student.getId())) {
            return ResponseEntity.status(cert == null ? 404 : 403)
                    .body(Map.of("success", false,
                                 "message", cert == null ? "Certificate not found"
                                                         : "Access denied"));
        }

        Object val = body.get("isPubliclyShareable");
        if (val == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "isPubliclyShareable field is required"));
        }

        boolean shareable = Boolean.parseBoolean(val.toString());
        cert.setIsPubliclyShareable(shareable);
        certificateRepository.save(cert);

        return ResponseEntity.ok(Map.of(
                "success",             true,
                "message",             shareable ? "Certificate is now public" : "Certificate is now private",
                "isPubliclyShareable", shareable
        ));
    }
}
