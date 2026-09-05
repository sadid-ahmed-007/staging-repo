package com.eduauth.controller;

import com.eduauth.controller.publics.VerifyController;
import com.eduauth.model.VerificationLog;
import com.eduauth.repository.VerificationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verifier-specific endpoints — all require ROLE_VERIFIER (enforced by
 * SecurityConfig).
 */
@RestController
@RequestMapping("/api/verifier")
@RequiredArgsConstructor
public class VerifierController {

    private final VerificationLogRepository verificationLogRepository;
    private final VerifyController verifyController;
    private final com.eduauth.repository.UserRepository userRepository;

    // ── Verifier Verification Stats ──────────────────────────────────────────

    @GetMapping("/verifications/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<?> verificationStats(@AuthenticationPrincipal UserDetails principal) {
        com.eduauth.model.User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null || user.getVerifier() == null) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a verifier"));
        }
        Long verifierId = user.getId();

        long total = verificationLogRepository.countByVerifierId(verifierId);
        long successful = verificationLogRepository.countByVerifierIdAndVerificationResult(verifierId, "success");
        long failed = verificationLogRepository.countFailedByVerifierId(verifierId);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long today = verificationLogRepository.countByVerifierIdToday(verifierId, startOfDay, endOfDay);

        List<VerificationLog> recentLogs = verificationLogRepository
                .findTop10ByVerifierIdOrderByVerifiedAtDesc(verifierId);
        List<Map<String, Object>> recent = recentLogs.stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("serial", log.getSerial());
            m.put("result", log.getVerificationResult());
            m.put("verified_at", log.getVerifiedAt() != null
                    ? log.getVerifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : null);
            m.put("details", log.getDetails());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_verifications", total);
        stats.put("successful_verifications", successful);
        stats.put("failed_verifications", failed);
        stats.put("verifications_today", today);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("stats", stats);
        response.put("recent_verifications", recent);
        return ResponseEntity.ok(response);
    }

    // ── Verifier Verify (logs under verifier account) ────────────────────────

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verify(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest httpRequest) {

        String serial = request.get("serial");
        String dateOfBirth = request.get("date_of_birth");

        if (serial == null || dateOfBirth == null) {
            return ResponseEntity.status(422).body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "serial and date_of_birth are required"));
        }

        com.eduauth.model.User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null || user.getVerifier() == null) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a verifier"));
        }
        Long verifierId = user.getId();

        // Delegate to the shared verification logic, passing the verifier's user ID
        return verifyController.doVerify(serial, dateOfBirth, verifierId, false, httpRequest);
    }

    // ── Recent Verifications (for sidebar panel) ─────────────────────────────

    @GetMapping("/verifications/recent")
    @Transactional(readOnly = true)
    public ResponseEntity<?> recentVerifications(@AuthenticationPrincipal UserDetails principal) {
        com.eduauth.model.User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null || user.getVerifier() == null) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a verifier"));
        }
        Long verifierId = user.getId();

        List<VerificationLog> logs = verificationLogRepository
                .findTop10ByVerifierIdOrderByVerifiedAtDesc(verifierId);

        List<Map<String, Object>> items = logs.stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("serial", log.getSerial());

            // Mask middle of serial: BSC-26-000001M → BSC-26-***001M
            String masked = maskSerial(log.getSerial());
            m.put("serial_masked", masked);

            // Student name — only if verification was successful and cert has student
            String studentName = null;
            String institution = null;
            if (log.getCertificate() != null && log.getCertificate().getStudent() != null
                    && "success".equals(log.getVerificationResult())) {
                String first = log.getCertificate().getStudent().getFirstName();
                String last = log.getCertificate().getStudent().getLastName();
                if (last != null && !last.isEmpty()) {
                    studentName = first + " " + last.substring(0, 1).toUpperCase() + ".";
                } else {
                    studentName = first;
                }
            }
            if (log.getCertificate() != null && log.getCertificate().getInstitution() != null) {
                institution = log.getCertificate().getInstitution().getName();
            }

            m.put("student_name", studentName);
            m.put("institution", institution);
            m.put("status", log.getVerificationResult());
            m.put("verified_at", log.getVerifiedAt() != null
                    ? log.getVerifiedAt().format(DateTimeFormatter.ISO_DATE_TIME)
                    : null);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "verifications", items));
    }

    // ── Verification History (paginated, filtered) ───────────────────────────

    @GetMapping("/verifications/history")
    @Transactional(readOnly = true)
    public ResponseEntity<?> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserDetails principal) {

        com.eduauth.model.User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null || user.getVerifier() == null) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Not a verifier"));
        }
        Long verifierId = user.getId();

        String statusParam = "all".equals(status) || status == null ? null : status;
        String serialParam = (serial != null && !serial.isBlank()) ? serial : null;
        LocalDateTime fromDate = parseDate(from, true);
        LocalDateTime toDate = parseDate(to, false);

        int pageSize = 15;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("verifiedAt").descending());

        Page<VerificationLog> pagedResult = verificationLogRepository.findFilteredHistory(
                verifierId, statusParam, serialParam, fromDate, toDate, pageable);

        List<Map<String, Object>> items = pagedResult.getContent().stream()
                .filter(log -> !"failed".equals(status) || !"success".equals(log.getVerificationResult()))
                .map(log -> {
                    boolean isSuccess = "success".equals(log.getVerificationResult());
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", log.getId());
                    m.put("serial", log.getSerial());

                    String studentName = null;
                    String institution = null;
                    if (isSuccess && log.getCertificate() != null) {
                        if (log.getCertificate().getStudent() != null) {
                            studentName = log.getCertificate().getStudent().getFirstName()
                                    + " " + log.getCertificate().getStudent().getLastName();
                        }
                        if (log.getCertificate().getInstitution() != null) {
                            institution = log.getCertificate().getInstitution().getName();
                        }
                    }

                    m.put("student_name", studentName);
                    m.put("institution", institution);
                    m.put("status", log.getVerificationResult());
                    m.put("verified_at", log.getVerifiedAt() != null
                            ? log.getVerifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            : null);
                    m.put("details", log.getDetails());

                    if (isSuccess && log.getCertificate() != null) {
                        Map<String, Object> cert = new HashMap<>();
                        cert.put("serial", log.getCertificate().getSerial());
                        cert.put("certificate_level", log.getCertificate().getCertificateName());
                        cert.put("program", log.getCertificate().getDepartment());
                        cert.put("major", log.getCertificate().getMajor());
                        cert.put("registration_no", log.getCertificate().getEnrollment() != null
                                ? log.getCertificate().getEnrollment().getEnrollmentNumber()
                                : null);
                        cert.put("cgpa", log.getCertificate().getCgpa());
                        cert.put("issue_date", log.getCertificate().getIssueDate() != null
                                ? log.getCertificate().getIssueDate().toString()
                                : null);
                        cert.put("completion_date", log.getCertificate().getConvocationDate() != null
                                ? log.getCertificate().getConvocationDate().toString()
                                : null);
                        cert.put("student_name", studentName);
                        cert.put("student_id", log.getCertificate().getEnrollment() != null
                                ? log.getCertificate().getEnrollment().getEnrollmentNumber()
                                : null);
                        cert.put("institution", institution);
                        m.put("certificate", cert);
                    } else {
                        m.put("certificate", null);
                    }

                    return m;
                }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", items);
        response.put("total", pagedResult.getTotalElements());
        response.put("per_page", pageSize);
        response.put("current_page", page);
        response.put("last_page", pagedResult.getTotalPages());
        return ResponseEntity.ok(response);
    }

    // ── Export CSV ────────────────────────────────────────────────────────────

    @GetMapping("/verifications/export")
    @Transactional(readOnly = true)
    public void exportVerifications(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse httpResponse) throws Exception {

        com.eduauth.model.User user = userRepository.findByEmail(principal.getUsername()).orElse(null);
        if (user == null || user.getVerifier() == null) {
            httpResponse.setStatus(403);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Not a verifier\"}");
            return;
        }
        Long verifierId = user.getId();

        String statusParam = "all".equals(status) ? null : status;
        String serialParam = (serial != null && !serial.isBlank()) ? serial : null;
        LocalDateTime fromDate = parseDate(from, true);
        LocalDateTime toDate = parseDate(to, false);

        List<VerificationLog> logs = verificationLogRepository.findFilteredForExport(
                verifierId, statusParam, serialParam, fromDate, toDate);

        String filename = "verifications_export_" + LocalDate.now().toString() + ".csv";
        httpResponse.setContentType("text/csv");
        httpResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        httpResponse.setCharacterEncoding("UTF-8");

        PrintWriter writer = httpResponse.getWriter();
        writer.println("Serial Number,Student Name,Institution,Status,Verified At,Details");

        for (VerificationLog log : logs) {
            String studentName = "—";
            String institution = "—";
            if (log.getCertificate() != null) {
                if (log.getCertificate().getStudent() != null) {
                    studentName = log.getCertificate().getStudent().getFirstName()
                            + " " + log.getCertificate().getStudent().getLastName();
                }
                if (log.getCertificate().getInstitution() != null) {
                    institution = log.getCertificate().getInstitution().getName();
                }
            }

            writer.println(String.join(",",
                    csvEscape(log.getSerial()),
                    csvEscape(studentName),
                    csvEscape(institution),
                    csvEscape(log.getVerificationResult()),
                    csvEscape(log.getVerifiedAt() != null
                            ? log.getVerifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            : ""),
                    csvEscape(log.getDetails() != null ? log.getDetails() : "")));
        }

        writer.flush();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String maskSerial(String serial) {
        if (serial == null || serial.length() < 11)
            return serial;
        // BSC-26-000001M → BSC-26-***001M
        StringBuilder sb = new StringBuilder(serial);
        for (int i = 7; i < serial.length() - 4; i++) {
            sb.setCharAt(i, '*');
        }
        return sb.toString();
    }

    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank()) {
            return isStart ? LocalDateTime.of(1900, 1, 1, 0, 0) : LocalDateTime.of(2100, 12, 31, 23, 59, 59);
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return isStart ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (Exception e) {
            return isStart ? LocalDateTime.of(1900, 1, 1, 0, 0) : LocalDateTime.of(2100, 12, 31, 23, 59, 59);
        }
    }

    private String csvEscape(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
