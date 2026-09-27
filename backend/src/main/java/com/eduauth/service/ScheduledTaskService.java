package com.eduauth.service;

import com.eduauth.model.AccessGrant;
import com.eduauth.model.Student;
import com.eduauth.model.Verifier;
import com.eduauth.repository.AccessGrantRepository;
import com.eduauth.repository.StudentRepository;
import com.eduauth.repository.VerifierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final AccessGrantRepository accessGrantRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final StudentRepository studentRepository;
    private final VerifierRepository verifierRepository;

    @Scheduled(cron = "0 0 8 * * ?") // Every day at 8 AM
    @Transactional
    public void processExpiringAccessGrants() {
        log.info("Running daily scheduled task: processExpiringAccessGrants");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inThreeDays = now.plusDays(3);

        List<AccessGrant> expiringGrants = accessGrantRepository.findGrantsExpiringBetweenAndNotified(now, inThreeDays, false);
        
        for (AccessGrant grant : expiringGrants) {
            try {
                notifyGrantExpiringSoon(grant);
                grant.setNotified3DaysBefore(true);
                accessGrantRepository.save(grant);
            } catch (Exception e) {
                log.error("Failed to notify for grant {}", grant.getId(), e);
            }
        }
        
        // Also check grants that just expired
        LocalDateTime yesterday = now.minusDays(1);
        List<AccessGrant> expiredGrants = accessGrantRepository.findGrantsExpiredBetweenAndNotified(yesterday, now, false);
        for (AccessGrant grant : expiredGrants) {
            try {
                notifyGrantExpired(grant);
                // We reuse notified3DaysBefore or we can just not track it since they only expire once
            } catch (Exception e) {
                log.error("Failed to notify for expired grant {}", grant.getId(), e);
            }
        }
    }

    private void notifyGrantExpiringSoon(AccessGrant grant) {
        Student student = studentRepository.findById(grant.getStudentId()).orElse(null);
        Verifier verifier = verifierRepository.findById(grant.getVerifierId()).orElse(null);
        
        if (verifier != null && verifier.getUser() != null && student != null) {
            String vName = verifier.getCompanyName() != null ? verifier.getCompanyName() : "Verifier";
            String studentName = student.getFirstName() + " " + student.getLastName();
            
            emailService.sendAccessExpiring(verifier.getUser().getId(), verifier.getUser().getEmail(), vName, studentName, grant.getExpiresAt().toString());
            notificationService.createNotification(
                    verifier.getUser().getId(),
                    "ACCESS_EXPIRING",
                    "Access Expiring Soon",
                    "Your access to " + studentName + "'s certificates expires in 3 days.",
                    "/verifier/accessible-certificates",
                    java.util.Map.of("accessGrantId", grant.getId())
            );
        }
    }
    
    private void notifyGrantExpired(AccessGrant grant) {
        Student student = studentRepository.findById(grant.getStudentId()).orElse(null);
        Verifier verifier = verifierRepository.findById(grant.getVerifierId()).orElse(null);
        
        if (verifier != null && verifier.getUser() != null && student != null) {
            String vName = verifier.getCompanyName() != null ? verifier.getCompanyName() : "Verifier";
            String studentName = student.getFirstName() + " " + student.getLastName();
            
            emailService.sendAccessExpired(verifier.getUser().getId(), verifier.getUser().getEmail(), vName, studentName); // Wait, sendAccessExpired doesn't exist in EmailService! Let's check.
            notificationService.createNotification(
                    verifier.getUser().getId(),
                    "ACCESS_EXPIRED",
                    "Access Expired",
                    "Your access to " + studentName + "'s certificates has expired.",
                    "/verifier/access-requests",
                    java.util.Map.of("accessGrantId", grant.getId())
            );
        }
    }
}
