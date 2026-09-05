package com.eduauth.controller.university;

import com.eduauth.model.Certificate;
import com.eduauth.model.Institution;
import com.eduauth.model.User;
import com.eduauth.repository.CertificateRepository;
import com.eduauth.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * University certificate endpoints.
 *
 * GET /api/university/certificates         → paginated list of own certificates
 * GET /api/university/certificates/{id}    → full details (must belong to this university)
 */
@RestController
@RequestMapping("/api/university/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('UNIVERSITY')")
public class UniversityCertificateController {

    private final InstitutionRepository   institutionRepository;
    private final CertificateRepository   certificateRepository;

    // ── GET /api/university/certificates ─────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> listCertificates(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "25")   int    size,
            @RequestParam(defaultValue = "all")  String level,
            @RequestParam(required = false)       String month,   // YYYY-MM
            @RequestParam(required = false)       String search) {

        Institution institution = institutionRepository.findByUserEmail(user.getEmail())
                .orElse(null);
        if (institution == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Institution profile not found"));
        }

        LocalDate monthStart = null;
        LocalDate monthEnd   = null;
        if (month != null && !month.isBlank()) {
            try {
                YearMonth ym = YearMonth.parse(month);
                monthStart = ym.atDay(1);
                monthEnd   = ym.atEndOfMonth();
            } catch (DateTimeParseException ignored) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid month format — use YYYY-MM"));
            }
        }

        String levelParam = ("all".equalsIgnoreCase(level) || level == null) ? null : level;
        Pageable pageable  = PageRequest.of(page, size);

        Page<Certificate> certPage = certificateRepository.findByInstitutionWithFilters(
                institution.getId(), levelParam, monthStart, monthEnd,
                (search != null && !search.isBlank()) ? search : null,
                pageable);

        List<Map<String, Object>> items = certPage.getContent().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",               c.getId());
                    m.put("serial",           c.getSerial());
                    m.put("studentName",      c.getStudentDisplayName());
                    m.put("studentEmail",     c.getStudent() != null && c.getStudent().getUser() != null
                                                ? c.getStudent().getUser().getEmail() : null);
                    m.put("certificateName",   c.getCertificateName());
                    m.put("certificateLevel",  c.getCertificateLevel());
                    m.put("cgpa",              c.getCgpa());
                    m.put("degreeClass",       c.getDegreeClass());
                    m.put("issueDate",         c.getIssueDate());
                    m.put("status",            c.isRevoked() ? "revoked" : "active");
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

    // ── GET /api/university/certificates/{id} ────────────────────────────────

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getCertificateDetails(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Institution institution = institutionRepository.findByUserEmail(user.getEmail())
                .orElse(null);
        if (institution == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Institution profile not found"));
        }

        Certificate cert = certificateRepository.findByIdWithDetails(id).orElse(null);
        if (cert == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Certificate not found"));
        }
        if (!cert.getInstitutionId().equals(institution.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false,
                                 "message", "Certificate does not belong to your institution"));
        }

        // Student info
        Map<String, Object> studentInfo = new LinkedHashMap<>();
        if (cert.getStudent() != null) {
            studentInfo.put("id",        cert.getStudent().getId());
            studentInfo.put("firstName",  cert.getStudent().getFirstName());
            studentInfo.put("lastName",   cert.getStudent().getLastName());
            studentInfo.put("email",      cert.getStudent().getUser() != null
                                            ? cert.getStudent().getUser().getEmail() : null);
        }

        // Enrollment info
        Map<String, Object> enrollmentInfo = null;
        if (cert.getEnrollment() != null) {
            enrollmentInfo = new LinkedHashMap<>();
            enrollmentInfo.put("enrollmentNumber",    cert.getEnrollment().getEnrollmentNumber());
            enrollmentInfo.put("rollNumber",          cert.getEnrollment().getRollNumber());
            enrollmentInfo.put("program",             cert.getEnrollment().getProgram());
            enrollmentInfo.put("batch",               cert.getEnrollment().getBatch());
            enrollmentInfo.put("status",              cert.getEnrollment().getStatus());
            enrollmentInfo.put("enrollmentDate",      cert.getEnrollment().getEnrollmentDate());
            enrollmentInfo.put("actualGraduationDate",cert.getEnrollment().getActualGraduationDate());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id",                  cert.getId());
        data.put("serial",              cert.getSerial());
        data.put("issuedName",          cert.getIssuedName());
        data.put("studentName",         cert.getStudentDisplayName());
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
        data.put("revokedAt",           cert.getRevokedAt());
        data.put("revokedByRole",       cert.getRevokedByRole());
        data.put("revocationReason",    cert.getRevocationReason());
        data.put("createdAt",           cert.getCreatedAt());
        data.put("student",             studentInfo);
        data.put("enrollment",          enrollmentInfo);

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }
}
