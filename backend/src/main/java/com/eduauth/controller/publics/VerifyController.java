package com.eduauth.controller.publics;

import com.eduauth.model.Certificate;
import com.eduauth.model.VerificationLog;
import com.eduauth.repository.CertificateRepository;
import com.eduauth.repository.UserRepository;
import com.eduauth.repository.VerificationLogRepository;
import com.eduauth.service.EncryptionService;
import com.eduauth.service.SerialGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Public certificate verification endpoints.
 * All endpoints are permitAll() — no authentication required.
 */
@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final EncryptionService encryptionService;

    // ── System stats for landing page ────────────────────────────────────────

    @GetMapping("/system-stats")
    public ResponseEntity<?> getSystemStats() {
        long totalUsers = userRepository.count();
        long totalUniversities = userRepository.countByRole("university");
        long totalCertificates = certificateRepository.count();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "totalSystemUsers", totalUsers,
                "totalUniversities", totalUniversities,
                "totalCertificates", totalCertificates));
    }

    // ── Public certificate verification (manual) ─────────────────────────────

    @PostMapping("/certificate")
    @Transactional
    public ResponseEntity<?> verifyCertificate(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String serial = request.get("serial");
        String dateOfBirthStr = request.get("date_of_birth");

        if (serial == null || dateOfBirthStr == null) {
            return ResponseEntity.status(422).body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "serial and date_of_birth are required"));
        }

        return doVerify(serial, dateOfBirthStr, null, false, httpRequest);
    }

    // ── Share link verification ──────────────────────────────────────────────

    @GetMapping("/link")
    @Transactional
    public ResponseEntity<?> verifyFromLink(
            @RequestParam("s") String serial,
            @RequestParam("v") String dobToken,
            HttpServletRequest httpRequest) {

        if (serial == null || dobToken == null) {
            return ResponseEntity.status(422).body(Map.of(
                    "success", false,
                    "verified", false,
                    "error", "Invalid verification link"));
        }

        String dob = encryptionService.decryptDOB(dobToken);
        if (dob == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "verified", false,
                    "error", "Invalid or expired verification link"));
        }

        return doVerify(serial, dob, null, true, httpRequest);
    }

    // ── Core verification logic ──────────────────────────────────────────────

    public ResponseEntity<?> doVerify(
            String serial,
            String dateOfBirthStr,
            Long verifierId,
            boolean isFromShareLink,
            HttpServletRequest httpRequest) {

        // Step 1: Validate checksum
        if (!SerialGeneratorService.validateChecksum(serial)) {
            logVerification(null, serial, dateOfBirthStr, false, "invalid_checksum",
                    "Invalid certificate serial number format", verifierId, httpRequest);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "Invalid certificate serial number format"));
        }

        // Step 2: Find certificate
        Optional<Certificate> certOpt = certificateRepository.findBySerial(serial);
        if (certOpt.isEmpty()) {
            logVerification(null, serial, dateOfBirthStr, false, "not_found",
                    "Certificate not found", verifierId, httpRequest);
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "Certificate not found with this serial number"));
        }

        Certificate certificate = certOpt.get();

        // Step 2.5: Check if explicitly marked private (Pre-DOB check)
        // We reject manual verification attempts early if the certificate is explicitly private
        // to prevent brute-forcing DOBs on private certificates.
        if (Boolean.FALSE.equals(certificate.getIsPubliclyShareable())) {
            logVerification(certificate.getId(), serial, dateOfBirthStr, false, "private",
                    "Certificate is marked as private", verifierId, httpRequest);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "This certificate is currently set to private by the student."));
        }

        // Step 3: Check if revoked
        if (certificate.getRevokedAt() != null) {
            logVerification(certificate.getId(), serial, dateOfBirthStr, false, "revoked",
                    "Certificate has been revoked", verifierId, httpRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("verified", false);
            response.put("status", "revoked");
            response.put("message", "This certificate has been revoked.");
            response.put("revoked_at", certificate.getRevokedAt().toLocalDate().toString());
            response.put("revocation_reason",
                    certificate.getRevocationReason() != null ? certificate.getRevocationReason() : "");
            response.put("revoked_by", certificate.getRevokedBy() != null
                    ? certificate.getRevokedBy().getRole().substring(0, 1).toUpperCase()
                      + certificate.getRevokedBy().getRole().substring(1)
                    : "Administrator");
            return ResponseEntity.ok(response);
        }

        // Step 4: Verify date of birth
        String studentDob = certificate.getStudent().getDateOfBirth()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (!studentDob.equals(dateOfBirthStr)) {
            logVerification(certificate.getId(), serial, dateOfBirthStr, false, "dob_mismatch",
                    "Date of birth did not match", verifierId, httpRequest);
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "Date of birth does not match our records"));
        }

        // Step 5: Check if publicly shareable (Post-DOB check)
        // Share links bypass this privacy check because the student explicitly generated 
        // the link with an encrypted DOB to share with someone. Manual verification is blocked.
        if (!isFromShareLink && !Boolean.TRUE.equals(certificate.getIsPubliclyShareable())) {
            logVerification(certificate.getId(), serial, dateOfBirthStr, true, "private_certificate",
                    "Certificate is private and not accessible for verification",
                    verifierId, httpRequest);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "verified", false,
                    "message", "This certificate is private and cannot be verified. "
                            + "The student has restricted access to this certificate."));
        }

        // Step 6: Success!
        logVerification(certificate.getId(), serial, dateOfBirthStr, true, "success",
                "Certificate verified successfully", verifierId, httpRequest);

        Map<String, Object> certDetails = buildCertificateDetails(certificate);

        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("verified", true);
        successResponse.put("message", "Certificate verified successfully");
        successResponse.put("certificate", certDetails);
        return ResponseEntity.ok(successResponse);
    }

    // ── Build certificate details map ────────────────────────────────────────

    private Map<String, Object> buildCertificateDetails(Certificate certificate) {
        Map<String, Object> d = new HashMap<>();
        d.put("serial", certificate.getSerial());
        d.put("student_name",
                certificate.getStudent() != null
                        ? certificate.getStudent().getFirstName() + " " + certificate.getStudent().getLastName()
                        : "N/A");
        d.put("student_id",
                certificate.getEnrollment() != null ? certificate.getEnrollment().getEnrollmentNumber() : "N/A");
        d.put("certificate_level", certificate.getCertificateName());
        d.put("program", certificate.getDepartment());
        d.put("major", certificate.getMajor());
        d.put("registration_no",
                certificate.getEnrollment() != null ? certificate.getEnrollment().getEnrollmentNumber() : "N/A");
        d.put("cgpa", certificate.getCgpa());
        d.put("issue_date",
                certificate.getIssueDate() != null ? certificate.getIssueDate().toString() : null);
        d.put("completion_date",
                certificate.getConvocationDate() != null ? certificate.getConvocationDate().toString() : null);
        d.put("institution",
                certificate.getInstitution() != null ? certificate.getInstitution().getName() : "N/A");
        d.put("issued_by",
                certificate.getIssuedBy() != null
                        ? certificate.getIssuedBy().getRole().substring(0, 1).toUpperCase()
                          + certificate.getIssuedBy().getRole().substring(1)
                        : "N/A");
        d.put("status", "valid");
        d.put("is_public", certificate.getIsPubliclyShareable());
        return d;
    }

    // ── Verification logging ─────────────────────────────────────────────────

    private void logVerification(
            Long certificateId, String serial, String dateOfBirthStr,
            boolean matchedByDob, String result, String details,
            Long verifierId, HttpServletRequest httpRequest) {

        LocalDate enteredDob = null;
        try {
            enteredDob = LocalDate.parse(dateOfBirthStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ignored) { }

        VerificationLog log = VerificationLog.builder()
                .certificateId(certificateId)
                .verifierId(verifierId)
                .serial(serial)
                .enteredDateOfBirth(enteredDob)
                .matchedByDob(matchedByDob)
                .verificationResult(result)
                .ipAddress(httpRequest != null ? httpRequest.getRemoteAddr() : null)
                .userAgent(httpRequest != null ? httpRequest.getHeader("User-Agent") : null)
                .details(details)
                .build();

        verificationLogRepository.save(log);
    }
}
