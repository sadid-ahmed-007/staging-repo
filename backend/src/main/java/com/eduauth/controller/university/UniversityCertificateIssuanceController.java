package com.eduauth.controller.university;

import com.eduauth.dto.certificate.BatchIssueResult;
import com.eduauth.dto.certificate.IssueCertificateRequest;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import com.eduauth.service.EnrollmentService;
import com.eduauth.service.NotificationService;
import com.eduauth.service.SerialGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * Certificate issuance endpoints for universities.
 *
 * POST /api/university/certificates               — issue single certificate
 * POST /api/university/certificates/batch         — batch issue from CSV
 * GET  /api/university/certificates/batch-template — download CSV template
 *
 * GET  endpoints are in UniversityCertificateController.java.
 */
@RestController
@RequestMapping("/api/university/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('UNIVERSITY')")
public class UniversityCertificateIssuanceController {

    private final InstitutionRepository   institutionRepository;
    private final CertificateRepository   certificateRepository;
    private final EnrollmentRepository    enrollmentRepository;
    private final StudentRepository       studentRepository;
    private final ActivityLogRepository   activityLogRepository;
    private final UserRepository          userRepository;
    private final EnrollmentService       enrollmentService;
    private final SerialGeneratorService  serialGeneratorService;
    private final NotificationService     notificationService;

    // ── POST /api/university/certificates ────────────────────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<?> issueCertificate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody IssueCertificateRequest req) {

        // 1. Resolve institution
        Institution institution = institutionRepository.findByUserId(user.getId()).orElse(null);
        if (institution == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Institution profile not found"));
        }

        // 2. Resolve student
        Student student = studentRepository.findById(req.getStudentId()).orElse(null);
        if (student == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Student not found"));
        }

        if (student.getUser() != null) {
            if (!Boolean.TRUE.equals(student.getUser().getIsApproved())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Cannot issue certificate: student account is not approved."));
            }
            if (student.getUser().getSuspendedAt() != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Cannot issue certificate: student account is suspended."));
            }
        }

        // 3. Resolve enrollment — prefer the explicit enrollmentId, otherwise find active
        Enrollment enrollment = null;
        if (req.getEnrollmentId() != null) {
            enrollment = enrollmentRepository.findById(req.getEnrollmentId()).orElse(null);
        } else {
            // Try to find any active enrollment at this institution
            List<Enrollment> institutionEnrollments =
                    enrollmentRepository.findByStudentIdAndInstitutionId(student.getId(), institution.getId());
            enrollment = institutionEnrollments.stream()
                    .filter(e -> "active".equals(e.getStatus()))
                    .findFirst()
                    .orElse(null);
        }

        if (enrollment == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false,
                            "message", "Student is not enrolled in your institution"));
        }
        if (!enrollment.getInstitutionId().equals(institution.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false,
                            "message", "Enrollment does not belong to your institution"));
        }

        // 4. Verify enrollment status is eligible (must be active)
        if (!"active".equals(enrollment.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false,
                            "message", "Cannot issue certificate: enrollment status is '"
                                    + enrollment.getStatus() + "'. Student must be active."));
        }

        // 4b. Verify either CGPA or Degree Class is provided
        boolean hasCgpa = req.getCgpa() != null;
        boolean hasDegreeClass = req.getDegreeClass() != null && !req.getDegreeClass().trim().isEmpty();
        if (!hasCgpa && !hasDegreeClass) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false,
                            "message", "Either CGPA or Degree Class is required before issuing a certificate."));
        }

        // 5. Resolve defaults from enrollment when request fields are blank
        String level = (req.getCertificateLevel() != null && !req.getCertificateLevel().isBlank())
                ? req.getCertificateLevel() : enrollment.getProgram();

        String certName = (req.getCertificateName() != null && !req.getCertificateName().isBlank())
                ? req.getCertificateName() : level;

        String dept = (req.getDepartment() != null && !req.getDepartment().isBlank())
                ? req.getDepartment() : enrollment.getProgram();

        String session = (req.getSession() != null && !req.getSession().isBlank())
                ? req.getSession() : enrollment.getBatch();

        LocalDate issueDate = req.getIssueDate() != null ? req.getIssueDate() : LocalDate.now();

        String authName = (req.getAuthorityName() != null && !req.getAuthorityName().isBlank())
                ? req.getAuthorityName() : institution.getName();

        String authTitle = (req.getAuthorityTitle() != null && !req.getAuthorityTitle().isBlank())
                ? req.getAuthorityTitle() : "Registrar";

        String issuedName = (req.getIssuedName() != null && !req.getIssuedName().isBlank())
                ? req.getIssuedName() : buildFullName(student);

        // 6. Generate serial via SerialGeneratorService (uses DB sequence + pessimistic lock)
        String serial = serialGeneratorService.generate(level);

        // 7. Persist certificate
        Certificate cert = new Certificate();
        cert.setStudentId(student.getId());
        cert.setInstitutionId(institution.getId());
        cert.setEnrollmentId(enrollment.getId());
        cert.setIssuedByUserId(user.getId());
        cert.setSerial(serial);
        cert.setCertificateLevel(level);
        cert.setCertificateName(certName);
        cert.setDepartment(dept);
        cert.setMajor(req.getMajor() != null ? req.getMajor() : null);
        cert.setSession(session);
        cert.setCgpa(req.getCgpa());
        cert.setDegreeClass(req.getDegreeClass());
        cert.setIssueDate(issueDate);
        cert.setConvocationDate(req.getConvocationDate());
        cert.setAuthorityName(authName);
        cert.setAuthorityTitle(authTitle);
        cert.setIssuedName(issuedName);
        cert.setIsPubliclyShareable(true);

        cert = certificateRepository.save(cert);

        // 8. Auto-graduate enrollment if still active
        if ("active".equals(enrollment.getStatus())) {
            enrollmentService.autoGraduate(enrollment.getId(), issueDate);
        }

        // 9. Log activity
        logActivity(user.getId(), "CERTIFICATE_ISSUED",
                "Issued certificate " + serial + " to student #" + student.getId()
                        + " (" + buildFullName(student) + ") at institution #" + institution.getId(),
                "Certificate", cert.getId());

        // 10. Notify student: CERTIFICATE_ISSUED
        if (student.getUser() != null) {
            notificationService.createNotification(
                    student.getUser().getId(),
                    "CERTIFICATE_ISSUED",
                    "Certificate Issued",
                    "Your certificate has been issued by " + institution.getName() + ".",
                    "/student/certificates",
                    Map.of("certificateId", cert.getId(), "serial", cert.getSerial())
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Certificate issued successfully. Enrollment marked as graduated.",
                "data", Map.of(
                        "id",               cert.getId(),
                        "serial",           cert.getSerial(),
                        "certificateLevel", cert.getCertificateLevel(),
                        "certificateName",  cert.getCertificateName(),
                        "issuedName",       cert.getIssuedName(),
                        "issueDate",        cert.getIssueDate(),
                        "enrollmentStatus", "graduated"
                )
        ));
    }

    // ── POST /api/university/certificates/batch ───────────────────────────────

    /**
     * Batch issue certificates from a CSV file.
     *
     * Expected CSV columns (first row = header):
     *   student_email, department, major, cgpa, degree_class
     *
     * All other fields (certificate name, level, session, authority, dates)
     * come from multipart form fields — NOT from the CSV.
     *
     * Process ALL rows even if some fail. Each success is committed independently.
     * Failed rows are reported with row number and reason.
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> batchIssueCertificates(
            @AuthenticationPrincipal User user,
            @RequestParam("certificateName")  String certificateName,
            @RequestParam("certificateLevel") String certificateLevel,
            @RequestParam("session")          String session,
            @RequestParam("authorityName")    String authorityName,
            @RequestParam("authorityTitle")   String authorityTitle,
            @RequestParam("issueDate")        String issueDateStr,
            @RequestParam(value = "convocationDate", required = false) String convocationDateStr,
            @RequestParam("csvFile")          MultipartFile csvFile) {

        // Resolve institution
        Institution institution = institutionRepository.findByUserId(user.getId()).orElse(null);
        if (institution == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Institution profile not found"));
        }

        // Parse issueDate
        LocalDate issueDate;
        try {
            issueDate = LocalDate.parse(issueDateStr); // expects YYYY-MM-DD
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false,
                            "message", "Invalid issueDate format — use YYYY-MM-DD"));
        }

        LocalDate convocationDate = null;
        if (convocationDateStr != null && !convocationDateStr.isBlank()) {
            try {
                convocationDate = LocalDate.parse(convocationDateStr);
            } catch (Exception ignored) { /* optional field — ignore parse error */ }
        }

        BatchIssueResult result = new BatchIssueResult();

        List<String[]> rows;
        String[] header;

        // Parse CSV
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {

            List<String[]> allLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                allLines.add(parseCsvLine(line));
            }

            if (allLines.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "CSV file is empty"));
            }

            header = allLines.get(0);
            rows   = allLines.subList(1, allLines.size());

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to parse CSV: " + e.getMessage()));
        }

        // Validate header columns
        List<String> expectedColumns = List.of("student_email", "department", "major", "cgpa", "degree_class");
        List<String> headerList = Arrays.stream(header).map(String::trim).map(String::toLowerCase).toList();
        for (String col : expectedColumns) {
            if (!col.equals("department") && !col.equals("major") && !col.equals("cgpa") && !col.equals("degree_class")) {
                if (!headerList.contains(col)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false,
                                    "message", "CSV is missing required column: " + col));
                }
            }
        }

        // Process each row
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // +2 because row 1 is header, rows are 1-indexed
            String[] rowValues = rows.get(i);

            if (rowValues.length != header.length) {
                result.addError(rowNumber, "N/A", "Column count mismatch");
                continue;
            }

            // Build map from header → value
            Map<String, String> data = new LinkedHashMap<>();
            for (int j = 0; j < header.length; j++) {
                data.put(header[j].trim().toLowerCase(), rowValues[j].trim());
            }

            String studentEmail = data.getOrDefault("student_email", "").trim();
            if (studentEmail.isEmpty()) {
                result.addError(rowNumber, "N/A", "student_email is required");
                continue;
            }

            // a) Find student by email
            User studentUser = userRepository.findByEmail(studentEmail).orElse(null);
            if (studentUser == null || !"student".equals(studentUser.getRole())) {
                result.addError(rowNumber, studentEmail, "Student not found");
                continue;
            }

            Student student = studentRepository.findByUserId(studentUser.getId()).orElse(null);
            if (student == null) {
                result.addError(rowNumber, studentEmail, "Student profile not found");
                continue;
            }

            if (!Boolean.TRUE.equals(studentUser.getIsApproved())) {
                result.addError(rowNumber, studentEmail, "Cannot issue certificate: student account is not approved");
                continue;
            }

            if (studentUser.getSuspendedAt() != null) {
                result.addError(rowNumber, studentEmail, "Cannot issue certificate: student account is suspended");
                continue;
            }

            // b) Verify enrollment at this institution
            List<Enrollment> enrollments =
                    enrollmentRepository.findByStudentIdAndInstitutionId(student.getId(), institution.getId());
            if (enrollments.isEmpty()) {
                result.addError(rowNumber, studentEmail,
                        "Student is not enrolled in your institution");
                continue;
            }

            // Check for active enrollment (student must be active before issuing certificate)
            Enrollment enrollment = enrollments.stream()
                    .filter(e -> "active".equals(e.getStatus()))
                    .findFirst()
                    .orElse(null);

            if (enrollment == null) {
                Enrollment latest = enrollments.get(0);
                result.addError(rowNumber, studentEmail,
                        "Cannot issue certificate: student enrollment status is '" + latest.getStatus() + "'. Student must be active.");
                continue;
            }

            // c) Parse optional CSV fields
            String dept        = data.getOrDefault("department", "").trim();
            String major       = data.getOrDefault("major", "").trim();
            String cgpaStr     = data.getOrDefault("cgpa", "").trim();
            String degreeClass = data.getOrDefault("degree_class", "").trim();

            if (dept.isEmpty()) dept = enrollment.getProgram();
            if (major.isEmpty()) major = null;
            if (degreeClass.isEmpty() || degreeClass.equalsIgnoreCase("n/a") || degreeClass.equalsIgnoreCase("none") || degreeClass.equals("-")) {
                degreeClass = null;
            }

            BigDecimal cgpa = null;
            if (!cgpaStr.isEmpty() && !cgpaStr.equalsIgnoreCase("n/a") && !cgpaStr.equalsIgnoreCase("none") && !cgpaStr.equals("-")) {
                try {
                    cgpa = new BigDecimal(cgpaStr);
                    if (cgpa.compareTo(BigDecimal.ZERO) < 0 || cgpa.compareTo(new BigDecimal("4.00")) > 0) {
                        result.addError(rowNumber, studentEmail, "cgpa must be between 0.00 and 4.00");
                        continue;
                    }
                } catch (NumberFormatException ex) {
                    result.addError(rowNumber, studentEmail, "Invalid cgpa value: " + cgpaStr);
                    continue;
                }
            }

            // Either cgpa or degree_class can be empty, but both cannot be empty
            if (cgpa == null && (degreeClass == null || degreeClass.trim().isEmpty())) {
                result.addError(rowNumber, studentEmail, "Either cgpa or degree_class is required (one of them must be present)");
                continue;
            }

            // d) Issue certificate (each row in its own independent transaction)
            try {
                String serial = issueOneInTransaction(
                        student, institution, enrollment, user,
                        certificateName, certificateLevel, dept, major, session,
                        cgpa, degreeClass, issueDate, convocationDate,
                        authorityName, authorityTitle);

                String studentName = buildFullName(student);
                result.addSuccess(studentEmail, serial, studentName);

            } catch (Exception ex) {
                result.addError(rowNumber, studentEmail, ex.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Processed " + result.getSuccessful() + " certificates, "
                        + result.getFailed() + " failed",
                "data",    result
        ));
    }

    // ── GET /api/university/certificates/batch-template ──────────────────────

    /**
     * Returns a downloadable CSV template for batch certificate issuance.
     * The CSV shows the required columns with example rows.
     */
    @GetMapping("/batch-template")
    public ResponseEntity<byte[]> downloadBatchTemplate() {
        String csv = "student_email,department,major,cgpa,degree_class\n"
                   + "student1@example.com,Computer Science and Engineering,Software Engineering,3.75,First Class\n"
                   + "student2@example.com,Computer Science and Engineering,Data Science,3.85,\n"
                   + "student3@example.com,Electrical and Electronic Engineering,Power Systems,,First Class\n";

        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "batch_template.csv");
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Issues a single certificate in its own @Transactional context so that
     * batch rows don't share a single rollback boundary.
     *
     * NOTE: Calling a @Transactional method from within the same bean bypasses
     * the proxy — so we use a dedicated sub-method called here and wired through
     * Spring's self-invocation pattern via the repository/service layer.
     * Each row is committed by calling save() + autoGraduate(), which carry their
     * own @Transactional annotations in the service/repository layer.
     */
    private String issueOneInTransaction(
            Student student,
            Institution institution,
            Enrollment enrollment,
            User issuedByUser,
            String certificateName,
            String certificateLevel,
            String department,
            String major,
            String session,
            BigDecimal cgpa,
            String degreeClass,
            LocalDate issueDate,
            LocalDate convocationDate,
            String authorityName,
            String authorityTitle) {

        String serial    = serialGeneratorService.generate(certificateLevel);
        String issuedName = buildFullName(student);

        Certificate cert = new Certificate();
        cert.setStudentId(student.getId());
        cert.setInstitutionId(institution.getId());
        cert.setEnrollmentId(enrollment.getId());
        cert.setIssuedByUserId(issuedByUser.getId());
        cert.setSerial(serial);
        cert.setCertificateLevel(certificateLevel);
        cert.setCertificateName(certificateName);
        cert.setDepartment(department);
        cert.setMajor(major);
        cert.setSession(session);
        cert.setCgpa(cgpa);
        cert.setDegreeClass(degreeClass);
        cert.setIssueDate(issueDate);
        cert.setConvocationDate(convocationDate);
        cert.setAuthorityName(authorityName);
        cert.setAuthorityTitle(authorityTitle);
        cert.setIssuedName(issuedName);
        cert.setIsPubliclyShareable(true);

        certificateRepository.save(cert);

        if ("active".equals(enrollment.getStatus())) {
            enrollmentService.autoGraduate(enrollment.getId(), issueDate);
        }

        logActivity(issuedByUser.getId(), "CERTIFICATE_BATCH_ISSUED",
                "Batch issued certificate " + serial + " to student #" + student.getId()
                        + " (" + issuedName + ")",
                "Certificate", cert.getId());

        // Notify student: CERTIFICATE_ISSUED
        if (student.getUser() != null) {
            notificationService.createNotification(
                    student.getUser().getId(),
                    "CERTIFICATE_ISSUED",
                    "Certificate Issued",
                    "Your certificate has been issued by " + institution.getName() + ".",
                    "/student/certificates",
                    Map.of("certificateId", cert.getId(), "serial", cert.getSerial())
            );
        }

        return serial;
    }

    private String buildFullName(Student s) {
        StringBuilder sb = new StringBuilder();
        if (s.getFirstName() != null) sb.append(s.getFirstName());
        if (s.getMiddleName() != null && !s.getMiddleName().isBlank()) {
            sb.append(" ").append(s.getMiddleName());
        }
        if (s.getLastName() != null) sb.append(" ").append(s.getLastName());
        return sb.toString().trim();
    }

    private void logActivity(Long userId, String action, String description,
                              String entityType, Long entityId) {
        try {
            ActivityLog log = new ActivityLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setDescription(description);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            activityLogRepository.save(log);
        } catch (Exception ignored) {
            // Never fail the main operation because of logging
        }
    }

    /**
     * Parse a single CSV line, respecting double-quoted fields.
     * Simple implementation — handles comma-separated values with optional quoting.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
