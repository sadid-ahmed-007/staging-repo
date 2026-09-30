package com.eduauth.service;

import com.eduauth.model.Certificate;
import com.eduauth.model.Verifier;
import com.eduauth.model.Student;
import com.eduauth.repository.VerifierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles post-verification notification logic shared between
 * PublicVerifyController (anonymous) and VerifierCertificateController
 * (logged-in verifier).
 *
 * Called ONLY after a result_status = "verified" / success outcome.
 *
 * Logic summary (from spec Part B):
 *  1. If notifyOnVerification == false → skip entirely.
 *  2. If verifierId is null → anonymous verifier.
 *  3. If notifyOnAnonymousOnly == true AND verifierId is NOT null → skip.
 *  4. Send in-app notification (CERTIFICATE_VERIFIED).
 *  5. Send email via EmailService.sendCertificateVerifiedEmail().
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationNotificationService {

    private final NotificationService notificationService;
    private final EmailService        emailService;
    private final VerifierRepository  verifierRepository;

    /**
     * Trigger all post-verification notifications for the student.
     *
     * @param certificate the certificate that was just verified
     * @param verifierId  null if anonymous, otherwise the Verifier.id (NOT user_id)
     */
    public void notifyStudent(Certificate certificate, Long verifierId) {
        try {
            // Step 1: Check master toggle
            if (!Boolean.TRUE.equals(certificate.getNotifyOnVerification())) {
                return;
            }

            // Step 2: Resolve verifier info
            boolean isAnonymous = (verifierId == null);
            String  companyName   = null;
            String  verifierEmail = null;

            if (!isAnonymous) {
                // verifierId is actually the User ID here (verifier_user_id)
                Verifier verifier = verifierRepository.findByUserId(verifierId).orElse(null);
                if (verifier != null) {
                    companyName   = verifier.getCompanyName();
                    verifierEmail = verifier.getUser() != null
                            ? verifier.getUser().getEmail()
                            : verifier.getEmail();
                }
            }

            // Step 3: notifyOnAnonymousOnly check
            if (Boolean.TRUE.equals(certificate.getNotifyOnAnonymousOnly()) && !isAnonymous) {
                // Only interested in anonymous alerts — skip logged-in verifier
                return;
            }

            // Step 4: In-app notification
            Student student = certificate.getStudent();
            if (student == null || student.getUser() == null) return;

            Long   studentUserId = student.getUser().getId();
            String certName      = certificate.getCertificateName() != null
                    ? certificate.getCertificateName() : "certificate";

            String inAppMessage = isAnonymous
                    ? "An anonymous user verified your " + certName + " certificate."
                    : (companyName != null ? companyName : "A verifier")
                      + " verified your " + certName + " certificate.";

            notificationService.createNotification(
                    studentUserId,
                    "CERTIFICATE_VERIFIED",
                    "Your certificate was verified",
                    inAppMessage,
                    "/student/certificates/" + certificate.getId(),
                    null
            );

            // Step 5: Email notification
            String studentEmail = student.getUser().getEmail();
            if (studentEmail == null || studentEmail.isBlank()) return;

            String studentName = certificate.getStudentDisplayName();

            // Build verifierInfo string (null = anonymous)
            String verifierInfo = null;
            if (!isAnonymous && companyName != null) {
                verifierInfo = companyName + "|" + (verifierEmail != null ? verifierEmail : "");
            }

            emailService.sendCertificateVerified(studentUserId, studentEmail, studentName, certificate.getCertificateName(), certificate.getSerial(), java.time.LocalDate.now().toString(), verifierInfo != null ? verifierInfo : "Anonymous");

        } catch (Exception ex) {
            // Notifications must never break the verification response
            log.warn("Failed to send verification notification for cert {}: {}",
                    certificate.getId(), ex.getMessage());
        }
    }
}
