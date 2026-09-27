package com.eduauth.service;

import com.eduauth.model.UserSettings;
import com.eduauth.repository.UserSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserSettingsRepository userSettingsRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.mail.from}")
    private String fromEmail;

    private boolean isEmailCategoryEnabled(Long userId, String category) {
        if (userId == null) return true; // Always send if we don't have a specific user
        if (category == null) return true; // Always send if no category specified
        
        try {
            Optional<UserSettings> settingsOpt = userSettingsRepository.findByUserId(userId);
            if (settingsOpt.isPresent() && settingsOpt.get().getPreferences() != null) {
                JsonNode prefs = objectMapper.readTree(settingsOpt.get().getPreferences());
                if (prefs.has("emailPreferences") && prefs.get("emailPreferences").has(category)) {
                    return prefs.get("emailPreferences").get(category).asBoolean();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse user settings for email preferences (userId: {}). Defaulting to true.", userId);
        }
        return true;
    }


    private void sendInternal(Long userId, String category, String toEmail, String subject, String templateName, Map<String, String> variables) {
        if (!isEmailCategoryEnabled(userId, category)) {
            log.info("Skipping email '{}' to {} because user disabled category '{}'", templateName, toEmail, category);
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + templateName);
            String htmlTemplate = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String val = entry.getValue() != null ? entry.getValue() : "";
                htmlTemplate = htmlTemplate.replace("{{" + entry.getKey() + "}}", val);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlTemplate, true);

            mailSender.send(message);
            log.info("Email '{}' sent successfully to: {}", templateName, toEmail);
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", templateName, toEmail, e.getMessage());
        }
    }

    public void sendOtpEmail(Long userId, String toEmail, String otp, String name) {
        sendInternal(userId, "accountEvents", toEmail, "Verify your email - EduAuth Registry", "otp.html", 
            Map.of(
            "toEmail", toEmail,
            "otp", otp,
            "name", name
            )
        );
    }

    public void sendEmailVerified(Long userId, String toEmail, String name) {
        sendInternal(userId, "accountEvents", toEmail, "Email verified - EduAuth Registry", "email-verified.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name
            )
        );
    }

    public void sendAccountApproved(Long userId, String toEmail, String name) {
        sendInternal(userId, "accountEvents", toEmail, "Your EduAuth Registry account has been approved", "account-approved.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name
            )
        );
    }

    public void sendAccountSuspended(Long userId, String toEmail, String name, String reason) {
        sendInternal(userId, "accountEvents", toEmail, "Account Suspended - EduAuth Registry", "account-suspended.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "reason", reason
            )
        );
    }

    public void sendAccountReactivated(Long userId, String toEmail, String name) {
        sendInternal(userId, "accountEvents", toEmail, "Account Reactivated - EduAuth Registry", "account-reactivated.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name
            )
        );
    }

    public void sendSecurityAlert(Long userId, String toEmail, String name, String date) {
        sendInternal(userId, "accountEvents", toEmail, "Security Alert: Password Changed", "security-alert.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "date", date
            )
        );
    }

    public void sendDeletionRequestedAdmin(Long userId, String toEmail, String email) {
        sendInternal(userId, "accountEvents", toEmail, "Account Deletion Requested", "deletion-requested-admin.html", 
            Map.of(
            "toEmail", toEmail,
            "email", email
            )
        );
    }

    public void sendDeletionRequestedUser(Long userId, String toEmail, String name) {
        sendInternal(userId, "accountEvents", toEmail, "Account Deletion Request Received", "deletion-requested-user.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name
            )
        );
    }

    public void sendDeletionCompleted(Long userId, String toEmail, String name) {
        sendInternal(userId, "accountEvents", toEmail, "Account Deleted", "deletion-completed.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name
            )
        );
    }

    public void sendDeletionCancelled(Long userId, String toEmail, String name) {
        sendInternal(userId, "accountEvents", toEmail, "Account Deletion Cancelled", "deletion-cancelled.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name
            )
        );
    }

    public void sendEnrollmentConfirmed(Long userId, String toEmail, String name, String universityName, String program, String department, String batch, String number, String date) {
        sendInternal(userId, "enrollmentEvents", toEmail, "Enrollment Confirmed - EduAuth Registry", "enrollment-confirmed.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "program", program,
            "department", department,
            "batch", batch,
            "number", number,
            "date", date
            )
        );
    }

    public void sendGraduationExtended(Long userId, String toEmail, String name, String universityName, String newDate, String reason) {
        sendInternal(userId, "enrollmentEvents", toEmail, "Graduation Date Extended", "graduation-extended.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "newDate", newDate,
            "reason", reason
            )
        );
    }

    public void sendWithdrawalRequested(Long userId, String toEmail, String studentName, String reason) {
        sendInternal(userId, "enrollmentEvents", toEmail, "Withdrawal Request Received", "withdrawal-requested.html", 
            Map.of(
            "toEmail", toEmail,
            "studentName", studentName,
            "reason", reason
            )
        );
    }

    public void sendWithdrawalApproved(Long userId, String toEmail, String name, String universityName, String responseMessage) {
        sendInternal(userId, "enrollmentEvents", toEmail, "Withdrawal Request Approved", "withdrawal-approved.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "responseMessage", responseMessage
            )
        );
    }

    public void sendWithdrawalRejected(Long userId, String toEmail, String name, String universityName, String responseMessage) {
        sendInternal(userId, "enrollmentEvents", toEmail, "Withdrawal Request Rejected", "withdrawal-rejected.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "responseMessage", responseMessage
            )
        );
    }

    public void sendStudentWithdrawn(Long userId, String toEmail, String name, String universityName, String reason) {
        sendInternal(userId, "enrollmentEvents", toEmail, "You have been withdrawn", "student-withdrawn.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "reason", reason
            )
        );
    }

    public void sendCertificateIssued(Long userId, String toEmail, String name, String universityName, String certificateName, String serial) {
        sendInternal(userId, "certificateEvents", toEmail, "New Certificate Issued", "certificate-issued.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "certificateName", certificateName,
            "serial", serial
            )
        );
    }

    public void sendCertificateVerified(Long userId, String toEmail, String name, String certificateName, String serial, String date, String verifierInfo) {
        sendInternal(userId, "verificationAlerts", toEmail, "Your certificate was verified", "certificate-verified.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "certificateName", certificateName,
            "serial", serial,
            "date", date,
            "verifierInfo", verifierInfo
            )
        );
    }

    public void sendCertificateRevoked(Long userId, String toEmail, String name, String certificateName, String serial, String revokedBy, String reason) {
        sendInternal(userId, "certificateEvents", toEmail, "Certificate Revoked", "certificate-revoked.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "certificateName", certificateName,
            "serial", serial,
            "revokedBy", revokedBy,
            "reason", reason
            )
        );
    }

    public void sendCertificateRevokedAdmin(Long userId, String toEmail, String studentName, String serial, String reason) {
        sendInternal(userId, "certificateEvents", toEmail, "Certificate Revoked by Admin", "certificate-revoked-admin.html", 
            Map.of(
            "toEmail", toEmail,
            "studentName", studentName,
            "serial", serial,
            "reason", reason
            )
        );
    }

    public void sendCertificateRevalidated(Long userId, String toEmail, String name, String certificateName, String serial) {
        sendInternal(userId, "certificateEvents", toEmail, "Certificate Revalidated", "certificate-revalidated.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "certificateName", certificateName,
            "serial", serial
            )
        );
    }

    public void sendAccessRequest(Long userId, String toEmail, String name, String companyName, String purpose, String days) {
        sendInternal(userId, "accessEvents", toEmail, "New Access Request", "access-request.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "companyName", companyName,
            "purpose", purpose,
            "days", days
            )
        );
    }

    public void sendAccessApproved(Long userId, String toEmail, String name, String studentName, String expiryDate) {
        sendInternal(userId, "accessEvents", toEmail, "Access Request Approved", "access-approved.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "studentName", studentName,
            "expiryDate", expiryDate
            )
        );
    }

    public void sendAccessRejected(Long userId, String toEmail, String name, String studentName) {
        sendInternal(userId, "accessEvents", toEmail, "Access Request Rejected", "access-rejected.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "studentName", studentName
            )
        );
    }

    public void sendAccessRevoked(Long userId, String toEmail, String name, String studentName) {
        sendInternal(userId, "accessEvents", toEmail, "Access Revoked", "access-revoked.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "studentName", studentName
            )
        );
    }

    public void sendAccessRevokedAdmin(Long userId, String toEmail, String name, String companyName, String studentName) {
        sendInternal(userId, "accessEvents", toEmail, "Access Revoked by Admin", "access-revoked-admin.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "companyName", companyName,
            "studentName", studentName
            )
        );
    }

    public void sendAccessExpiring(Long userId, String toEmail, String name, String studentName, String expiryDate) {
        sendInternal(userId, "accessEvents", toEmail, "Access Expiring Soon", "access-expiring.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "studentName", studentName,
            "expiryDate", expiryDate
            )
        );
    }


    public void sendAccessExpired(Long userId, String toEmail, String name, String studentName) {
        sendInternal(userId, "accessEvents", toEmail, "Access Expired", "access-expired.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "studentName", studentName
            )
        );
    }

    public void sendApplicationSubmitted(Long userId, String toEmail, String studentName, String programName) {
        sendInternal(userId, "applicationEvents", toEmail, "New Application Received", "application-submitted.html", 
            Map.of(
            "toEmail", toEmail,
            "studentName", studentName,
            "programName", programName
            )
        );
    }

    public void sendApplicationAccepted(Long userId, String toEmail, String name, String universityName, String programName, String acceptanceMessage) {
        sendInternal(userId, "applicationEvents", toEmail, "Application Accepted!", "application-accepted.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "programName", programName,
            "acceptanceMessage", acceptanceMessage
            )
        );
    }

    public void sendApplicationRejected(Long userId, String toEmail, String name, String universityName, String programName, String rejectionReason) {
        sendInternal(userId, "applicationEvents", toEmail, "Application Update", "application-rejected.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "universityName", universityName,
            "programName", programName,
            "rejectionReason", rejectionReason
            )
        );
    }

    public void sendApplicationCancelled(Long userId, String toEmail, String studentName, String programName) {
        sendInternal(userId, "applicationEvents", toEmail, "Application Cancelled", "application-cancelled.html", 
            Map.of(
            "toEmail", toEmail,
            "studentName", studentName,
            "programName", programName
            )
        );
    }

    public void sendProfileChangeRequest(Long userId, String toEmail, String userName, String role, String fieldName) {
        sendInternal(userId, "accountEvents", toEmail, "Profile Change Request", "profile-change-request.html", 
            Map.of(
            "toEmail", toEmail,
            "userName", userName,
            "role", role,
            "fieldName", fieldName
            )
        );
    }

    public void sendProfileChangeApproved(Long userId, String toEmail, String name, String fieldName) {
        sendInternal(userId, "accountEvents", toEmail, "Profile Change Approved", "profile-change-approved.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "fieldName", fieldName
            )
        );
    }

    public void sendProfileChangeRejected(Long userId, String toEmail, String name, String fieldName, String reviewNotes) {
        sendInternal(userId, "accountEvents", toEmail, "Profile Change Rejected", "profile-change-rejected.html", 
            Map.of(
            "toEmail", toEmail,
            "name", name,
            "fieldName", fieldName,
            "reviewNotes", reviewNotes
            )
        );
    }
}
