package com.eduauth.service;

import com.eduauth.dto.dashboard.*;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final UserRepository userRepository;

    public StudentDashboardDto getStudentDashboardStats(User user) {
        StudentDashboardDto dto = new StudentDashboardDto();
        User managedUser = userRepository.findById(user.getId()).orElseThrow();
        Long studentId = managedUser.getStudent().getId();
        
        dto.setTotalCertificates(certificateRepository.countByStudentId(studentId));
        dto.setPublicCertificates(certificateRepository.countByStudentIdAndIsPubliclyShareableTrue(studentId));
        dto.setPrivateCertificates(certificateRepository.countByStudentIdAndIsPubliclyShareableFalse(studentId));
        
        dto.setPendingAccessRequests(accessRequestRepository.countByStudentIdAndStatus(studentId, "pending"));
        dto.setActiveAccessGrants(accessGrantRepository.countActiveGrantsForStudent(studentId, LocalDateTime.now()));
        
        enrollmentRepository.findFirstByStudentIdAndStatusOrderByEnrollmentDateDesc(studentId, "active")
            .ifPresent(enrollment -> {
                StudentDashboardDto.CurrentEnrollmentDto curr = new StudentDashboardDto.CurrentEnrollmentDto();
                if (enrollment.getInstitution() != null) {
                    curr.setInstitutionName(enrollment.getInstitution().getName());
                }
                curr.setProgram(enrollment.getProgram());
                curr.setBatch(enrollment.getBatch());
                curr.setStatus(enrollment.getStatus());
                curr.setEnrollmentDate(enrollment.getEnrollmentDate());
                curr.setExpectedGraduationDate(enrollment.getExpectedGraduationDate());
                dto.setCurrentEnrollment(curr);
            });
            
        return dto;
    }

    public UniversityDashboardDto getUniversityDashboardStats(User user) {
        UniversityDashboardDto dto = new UniversityDashboardDto();
        User managedUser = userRepository.findById(user.getId()).orElseThrow();
        Long institutionId = managedUser.getInstitution().getId();
        
        dto.setTotalEnrolled(enrollmentRepository.countByInstitutionIdAndStatus(institutionId, "active"));
        dto.setGraduatedStudents(enrollmentRepository.countByInstitutionIdAndStatus(institutionId, "graduated"));
        dto.setCertificatesIssued(certificateRepository.countByInstitutionId(institutionId));
        dto.setPendingWithdrawals(withdrawalRequestRepository.countByInstitutionIdAndStatus(institutionId, "pending"));
        
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        dto.setThisMonthCertificates(certificateRepository.countByInstitutionIdAndIssueDateBetween(institutionId, startOfMonth, endOfMonth));
        
        return dto;
    }

    public VerifierDashboardDto getVerifierDashboardStats(User user) {
        VerifierDashboardDto dto = new VerifierDashboardDto();
        User managedUser = userRepository.findById(user.getId()).orElseThrow();
        Long verifierId = managedUser.getVerifier().getId();
        
        dto.setAccessibleStudents(accessGrantRepository.countActiveGrantsForVerifier(verifierId, LocalDateTime.now()));
        dto.setPendingRequests(accessRequestRepository.countByVerifierIdAndStatus(verifierId, "pending"));
        dto.setApprovedRequests(accessRequestRepository.countByVerifierIdAndStatus(verifierId, "approved"));
        dto.setRejectedRequests(accessRequestRepository.countByVerifierIdAndStatus(verifierId, "rejected"));
        
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        
        dto.setVerificationsToday(verificationLogRepository.countByVerifierIdAndCreatedAtBetween(verifierId, startOfDay, endOfDay));
        dto.setTotalVerifications(verificationLogRepository.countByVerifierId(verifierId));
        
        return dto;
    }

    public AdminDashboardDto getAdminDashboardStats() {
        AdminDashboardDto dto = new AdminDashboardDto();
        
        dto.setPendingApprovals(userRepository.countByIsApprovedFalseAndEmailVerifiedAtIsNotNull());
        dto.setTotalUsers(userRepository.countByIsApprovedTrue());
        dto.setTotalCertificates(certificateRepository.count());
        dto.setTotalUniversities(userRepository.countByRole("university"));
        dto.setTotalStudents(userRepository.countByRole("student"));
        dto.setTotalVerifiers(userRepository.countByRole("verifier"));
        
        return dto;
    }
}
