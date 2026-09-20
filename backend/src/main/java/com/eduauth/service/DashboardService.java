package com.eduauth.service;

import com.eduauth.dto.dashboard.*;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final InstitutionRepository institutionRepository;
    private final CertificateLevelRepository certificateLevelRepository;
    private final ActivityLogRepository activityLogRepository;

    public StudentDashboardDto getStudentDashboardStats(User user) {
        StudentDashboardDto dto = new StudentDashboardDto();
        User managedUser = userRepository.findById(user.getId()).orElseThrow();
        Long studentId = managedUser.getStudent().getId();
        
        dto.setTotalCertificates(certificateRepository.countByStudentId(studentId));
        dto.setPublicCertificates(certificateRepository.countByStudentIdAndIsPubliclyShareableTrue(studentId));
        dto.setPrivateCertificates(certificateRepository.countByStudentIdAndIsPubliclyShareableFalse(studentId));
        
        dto.setPendingAccessRequests(accessRequestRepository.countByStudentIdAndStatus(studentId, "pending"));
        dto.setActiveAccessGrants(accessGrantRepository.countActiveGrantsForStudent(studentId, LocalDateTime.now()));
        
        // Find active or withdrawal_requested enrollment
        Enrollment enrollment = enrollmentRepository.findActiveByStudentId(studentId).orElse(null);
        if (enrollment == null) {
            enrollment = enrollmentRepository.findFirstByStudentIdAndStatusOrderByEnrollmentDateDesc(studentId, "active").orElse(null);
        }

        if (enrollment != null) {
            StudentDashboardDto.CurrentEnrollmentDto curr = new StudentDashboardDto.CurrentEnrollmentDto();
            Institution inst = institutionRepository.findById(enrollment.getInstitutionId()).orElse(null);
            curr.setInstitutionName(inst != null ? inst.getName() : "University");
            curr.setEnrollmentNumber(enrollment.getEnrollmentNumber());
            curr.setProgram(enrollment.getProgram());
            curr.setBatch(enrollment.getBatch());

            boolean isWithdrawalPending = withdrawalRequestRepository
                    .existsByEnrollmentIdAndStatus(enrollment.getId(), "pending");
            curr.setStatus(isWithdrawalPending ? "withdrawal_requested" : enrollment.getStatus());
            curr.setEnrollmentDate(enrollment.getEnrollmentDate());
            curr.setExpectedGraduationDate(enrollment.getExpectedGraduationDate());
            dto.setCurrentEnrollment(curr);
        } else {
            dto.setCurrentEnrollment(null);
        }
            
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

        // Program Overview Breakdown
        List<CertificateLevel> levels = certificateLevelRepository.findByInstitutionIdAndIsActiveTrue(institutionId);
        List<Enrollment> enrollments = enrollmentRepository.findByInstitutionIdAndStatus(institutionId, "active");
        List<Certificate> certificates = certificateRepository.findByInstitutionId(institutionId);

        List<UniversityDashboardDto.ProgramBreakdownDto> breakdown = new java.util.ArrayList<>();
        for (CertificateLevel lvl : levels) {
            String shortName = lvl.getShortCode() != null ? lvl.getShortCode() : lvl.getName();
            String fullName = lvl.getName();
            String prefix = lvl.getSerialPrefix() != null ? lvl.getSerialPrefix().toUpperCase() : "";

            long activeCount = enrollments.stream().filter(e -> {
                if (e.getCertificateLevelId() != null && e.getCertificateLevelId().equals(lvl.getId())) {
                    return true;
                }
                String prog = e.getProgram() != null ? e.getProgram().toLowerCase() : "";
                return prog.contains(shortName.toLowerCase()) || prog.contains(fullName.toLowerCase());
            }).count();

            long certCount = certificates.stream().filter(c -> {
                String certLvl = c.getCertificateLevel() != null ? c.getCertificateLevel().toLowerCase() : "";
                String serial = c.getSerial() != null ? c.getSerial().toUpperCase() : "";
                if (!prefix.isEmpty() && serial.startsWith(prefix)) {
                    return true;
                }
                return certLvl.equalsIgnoreCase(shortName) || certLvl.equalsIgnoreCase(fullName) || certLvl.contains(shortName.toLowerCase());
            }).count();

            breakdown.add(UniversityDashboardDto.ProgramBreakdownDto.builder()
                    .levelName(fullName)
                    .shortName(shortName)
                    .activeStudents(activeCount)
                    .certsIssued(certCount)
                    .build());
        }
        dto.setProgramBreakdown(breakdown);
        
        return dto;
    }

    public List<Map<String, Object>> getUniversityRecentActivity(User user) {
        Page<ActivityLog> paged = activityLogRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 10));

        return paged.getContent().stream().map(log -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", log.getId());
            map.put("action", log.getAction());
            map.put("description", log.getDescription());
            map.put("createdAt", log.getCreatedAt());
            map.put("created_at", log.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
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
        dto.setTotalEnrollments(enrollmentRepository.countByStatus("active"));
        
        return dto;
    }
}
