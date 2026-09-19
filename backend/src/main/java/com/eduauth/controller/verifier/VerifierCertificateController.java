package com.eduauth.controller.verifier;

import com.eduauth.model.AccessGrant;
import com.eduauth.model.Certificate;
import com.eduauth.model.Student;
import com.eduauth.model.User;
import com.eduauth.model.Verifier;
import com.eduauth.repository.AccessGrantRepository;
import com.eduauth.repository.AccessRequestRepository;
import com.eduauth.repository.CertificateRepository;
import com.eduauth.repository.StudentRepository;
import com.eduauth.repository.VerifierRepository;
import com.eduauth.service.CertificateService;
import com.eduauth.model.Enrollment;
import com.eduauth.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifier endpoints for accessible certificates.
 *
 * GET /api/verifier/accessible-certificates                      → list of students this verifier has access to
 * GET /api/verifier/accessible-certificates/{studentId}          → all certs for a specific student
 * GET /api/verifier/accessible-certificates/certificates/{id}/pdf → PDF download for shareable accessible cert
 */
@RestController
@RequestMapping("/api/verifier/accessible-certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VERIFIER')")
@Transactional(readOnly = true)
public class VerifierCertificateController {

    private final VerifierRepository      verifierRepository;
    private final AccessGrantRepository   accessGrantRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final StudentRepository       studentRepository;
    private final CertificateRepository   certificateRepository;
    private final EnrollmentRepository    enrollmentRepository;
    private final CertificateService      certificateService;

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

        // Group grants by studentId
        Map<Long, List<AccessGrant>> grantsByStudent = grants.stream()
                .collect(Collectors.groupingBy(AccessGrant::getStudentId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> students = new ArrayList<>();
        for (Map.Entry<Long, List<AccessGrant>> entry : grantsByStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<AccessGrant> studentGrants = entry.getValue();

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) continue;

            boolean hasAllAccess = studentGrants.stream().anyMatch(g -> g.getCertificateId() == null);
            Set<Long> specificCertIds = studentGrants.stream()
                    .map(AccessGrant::getCertificateId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            long certCount = hasAllAccess
                    ? certificateRepository.countByStudentIdAndRevokedAtIsNull(student.getId())
                    : specificCertIds.size();

            // Determine institution and department
            String institutionName = null;
            String department = null;
            Enrollment activeEnrollment = enrollmentRepository.findActiveByStudentId(student.getId()).orElse(null);
            if (activeEnrollment != null) {
                if (activeEnrollment.getInstitution() != null) {
                    institutionName = activeEnrollment.getInstitution().getName();
                }
                department = activeEnrollment.getProgram();
            }

            if (institutionName == null && certCount > 0) {
                List<Certificate> certs = certificateRepository.findByStudentIdOrderByIssueDateDesc(student.getId());
                if (!certs.isEmpty()) {
                    Certificate firstCert = certs.get(0);
                    if (firstCert.getInstitution() != null) {
                        institutionName = firstCert.getInstitution().getName();
                    }
                    if (department == null) {
                        department = firstCert.getDepartment();
                    }
                }
            }

            LocalDateTime latestExpires = studentGrants.stream()
                    .map(AccessGrant::getExpiresAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime earliestGranted = studentGrants.stream()
                    .map(AccessGrant::getGrantedAt)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId",           student.getId());
            item.put("studentName",         buildFullName(student));
            item.put("studentEmail",        student.getUser() != null ? student.getUser().getEmail() : null);
            item.put("institutionName",     institutionName);
            item.put("department",          department);
            item.put("accessGrantedAt",     earliestGranted);
            item.put("accessExpiresAt",     latestExpires);
            item.put("certificateCount",    certCount);
            item.put("hasAllAccess",        hasAllAccess);
            item.put("isSpecificOnly",      !hasAllAccess);
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
        List<AccessGrant> activeGrants = accessGrantRepository
                .findAllActiveGrantsForVerifierAndStudent(verifier.getId(), studentId, now);

        if (activeGrants.isEmpty()) {
            return ResponseEntity.status(403)
                     .body(Map.of("success", false,
                                 "message", "You do not have active access to this student's certificates. Access may have expired or been revoked."));
        }

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student not found"));
        }

        boolean hasAllAccess = activeGrants.stream().anyMatch(g -> g.getCertificateId() == null);
        Set<Long> allowedCertIds = activeGrants.stream()
                .map(AccessGrant::getCertificateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Return non-revoked certs only, filtered by grant scope
        List<Certificate> certs = certificateRepository
                .findByStudentIdAndRevokedAtIsNullOrderByIssueDateDesc(studentId);

        if (!hasAllAccess) {
            certs = certs.stream()
                    .filter(c -> allowedCertIds.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        String studentFullName = buildFullName(student);
        String studentEmail = student.getUser() != null ? student.getUser().getEmail() : null;

        List<Map<String, Object>> items = certs.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",                   c.getId());
                    m.put("serial",               c.getSerial());
                    m.put("certificateName",      c.getCertificateName());
                    m.put("certificateLevel",     c.getCertificateLevel());
                    m.put("institutionName",      c.getInstitution() != null ? c.getInstitution().getName() : null);
                    m.put("department",           c.getDepartment());
                    m.put("major",                c.getMajor());
                    m.put("cgpa",                 c.getCgpa());
                    m.put("degreeClass",          c.getDegreeClass());
                    m.put("issueDate",            c.getIssueDate());
                    m.put("status",               "active");
                    m.put("isPubliclyShareable",  c.getIsPubliclyShareable());
                    m.put("createdAt",            c.getCreatedAt());

                    m.put("studentName",          studentFullName);
                    m.put("studentEmail",         studentEmail);
                    if (c.getEnrollment() != null) {
                        m.put("rollNumber",       c.getEnrollment().getRollNumber());
                        m.put("enrollmentNumber", c.getEnrollment().getEnrollmentNumber());
                        m.put("program",          c.getEnrollment().getProgram());
                    }
                    return m;
                })
                .collect(Collectors.toList());

        LocalDateTime maxExpires = activeGrants.stream()
                .map(AccessGrant::getExpiresAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        boolean hasPendingAllRequest = accessRequestRepository.existsPendingRequestForAll(verifier.getId(), studentId);

        Map<String, Object> studentInfo = new LinkedHashMap<>();
        studentInfo.put("id",                    student.getId());
        studentInfo.put("name",                  studentFullName);
        studentInfo.put("email",                 studentEmail);
        studentInfo.put("hasAllAccess",          hasAllAccess);
        studentInfo.put("hasPendingAllRequest",  hasPendingAllRequest);

        return ResponseEntity.ok(Map.of(
                "success",       true,
                "student",       studentInfo,
                "accessExpires", maxExpires,
                "data",          items,
                "total",         items.size()
        ));
    }

    // ── GET /api/verifier/accessible-certificates/certificates/{id} ───────────

    @GetMapping("/certificates/{id}")
    public ResponseEntity<?> getCertificateDetails(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Verifier profile not found"));
        }

        Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
        if (cert == null || cert.getRevokedAt() != null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Certificate not found or has been revoked"));
        }

        LocalDateTime now = LocalDateTime.now();
        List<AccessGrant> matchingGrants = accessGrantRepository
                .findActiveGrantsForCertificate(verifier.getId(), cert.getStudentId(), cert.getId(), now);

        if (matchingGrants.isEmpty()) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "You do not have active access to this certificate"));
        }

        Student student = cert.getStudent();
        String studentFullName = student != null ? buildFullName(student) : null;
        String studentEmail = (student != null && student.getUser() != null) ? student.getUser().getEmail() : null;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                   cert.getId());
        m.put("serial",               cert.getSerial());
        m.put("certificateName",      cert.getCertificateName());
        m.put("certificateLevel",     cert.getCertificateLevel());
        m.put("institutionName",      cert.getInstitution() != null ? cert.getInstitution().getName() : null);
        m.put("department",           cert.getDepartment());
        m.put("major",                cert.getMajor());
        m.put("cgpa",                 cert.getCgpa());
        m.put("degreeClass",          cert.getDegreeClass());
        m.put("issueDate",            cert.getIssueDate());
        m.put("status",               "active");
        m.put("isPubliclyShareable",  cert.getIsPubliclyShareable());
        m.put("createdAt",            cert.getCreatedAt());
        m.put("studentName",          studentFullName);
        m.put("studentEmail",         studentEmail);
        if (cert.getEnrollment() != null) {
            m.put("rollNumber",       cert.getEnrollment().getRollNumber());
            m.put("enrollmentNumber", cert.getEnrollment().getEnrollmentNumber());
            m.put("program",          cert.getEnrollment().getProgram());
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data",    m
        ));
    }

    // ── GET /api/verifier/accessible-certificates/certificates/{id}/pdf ──────

    @GetMapping("/certificates/{id}/pdf")
    public ResponseEntity<byte[]> getCertificatePdf(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
        if (verifier == null) {
            return ResponseEntity.status(404).build();
        }

        Certificate cert = certificateRepository.findById(id).orElse(null);
        if (cert == null || cert.getRevokedAt() != null) {
            return ResponseEntity.status(404).build();
        }

        // Only allow download if isPubliclyShareable is true
        if (!Boolean.TRUE.equals(cert.getIsPubliclyShareable())) {
            return ResponseEntity.status(403).build();
        }

        // Check active access grant (covers all or this certificate)
        LocalDateTime now = LocalDateTime.now();
        List<AccessGrant> matchingGrants = accessGrantRepository
                .findActiveGrantsForCertificate(verifier.getId(), cert.getStudent().getId(), cert.getId(), now);

        if (matchingGrants.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        return certificateService.generatePdf(cert);
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
