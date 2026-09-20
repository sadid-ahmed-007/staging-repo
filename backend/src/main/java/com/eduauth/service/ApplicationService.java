package com.eduauth.service;

import com.eduauth.dto.application.ApplicationResponse;
import com.eduauth.dto.application.ReviewApplicationRequest;
import com.eduauth.dto.application.SubmitApplicationRequest;
import com.eduauth.exception.BadRequestException;
import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the student → university application lifecycle.
 *
 * <p>Status flow: pending → accepted | rejected | cancelled</p>
 *
 * <p>Accepting an application does NOT auto-create an enrollment.
 * The university must still enroll the student manually via the enrollment
 * form, which can be pre-filled using the {@code enrollmentHint} returned
 * by the review endpoint.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final UniversityApplicationRepository applicationRepository;
    private final StudentRepository               studentRepository;
    private final InstitutionRepository           institutionRepository;
    private final EnrollmentRepository            enrollmentRepository;
    private final WithdrawalRequestRepository     withdrawalRequestRepository;
    private final CertificateRepository           certificateRepository;
    private final ProgramRepository               programRepository;
    private final DepartmentRepository            departmentRepository;
    private final CertificateLevelRepository      certificateLevelRepository;
    private final UserRepository                  userRepository;
    private final NotificationService             notificationService;
    private final ActivityLogRepository           activityLogRepository;
    private final com.eduauth.repository.AccessGrantRepository accessGrantRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT APPLICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ApplicationResponse submitApplication(SubmitApplicationRequest dto, Long userId) {

        // Step 1: Resolve student and verify account is approved
        Student student = studentRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        User studentUser = student.getUser();
        if (!Boolean.TRUE.equals(studentUser.getIsApproved())) {
            throw new BadRequestException("Your account must be approved before you can apply to universities.");
        }
        if (studentUser.getSuspendedAt() != null) {
            throw new BadRequestException("Your account is currently suspended. Please contact support.");
        }

        // Step 2: Verify university exists and is approved
        Institution university = institutionRepository.findById(dto.getUniversityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "University not found: " + dto.getUniversityId()));

        User universityUser = university.getUser();
        if (universityUser == null || !Boolean.TRUE.equals(universityUser.getIsApproved())) {
            throw new BadRequestException("This university is not yet approved and cannot accept applications.");
        }

        // Step 3: Check for existing pending application to this university
        if (applicationRepository.existsByStudentIdAndUniversityIdAndStatus(
                student.getId(), university.getId(), "pending")) {
            throw new BadRequestException(
                    "You already have a pending application to this university.");
        }

        // Step 4: Check if student is already actively enrolled anywhere
        boolean hasActiveEnrollment = enrollmentRepository
                .existsByStudentIdAndStatus(student.getId(), "active");

        if (!hasActiveEnrollment) {
            // Also treat active + pending-withdrawal as "enrolled"
            List<Enrollment> active = enrollmentRepository.findByStudentIdAndStatus(student.getId(), "active");
            hasActiveEnrollment = active.stream().anyMatch(e ->
                    withdrawalRequestRepository.existsByEnrollmentIdAndStatus(e.getId(), "pending"));
        }

        if (hasActiveEnrollment) {
            throw new BadRequestException(
                    "You cannot apply while enrolled at another institution. " +
                    "Please complete or withdraw from your current enrollment first.");
        }

        // Step 5: If programId provided, validate it belongs to this university
        if (dto.getProgramId() != null) {
            Program program = programRepository.findById(dto.getProgramId()).orElse(null);
            if (program == null || !university.getId().equals(program.getUniversityId())) {
                throw new BadRequestException(
                        "The specified program does not belong to this university.");
            }
        }

        // Step 6: Create application record
        UniversityApplication application = new UniversityApplication();
        application.setStudentId(student.getId());
        application.setUniversityId(university.getId());
        application.setCertificateLevelId(dto.getCertificateLevelId());
        application.setDepartmentId(dto.getDepartmentId());
        application.setProgramId(dto.getProgramId());
        application.setPersonalStatement(dto.getPersonalStatement());
        application.setStatus("pending");

        application = applicationRepository.save(application);

        // Step 7: Notify university
        String studentName = buildStudentName(student);
        String targetName  = resolveTargetName(dto.getProgramId(), dto.getCertificateLevelId());

        notificationService.createNotification(
                universityUser.getId(),
                "NEW_APPLICATION",
                "New Application Received",
                studentName + " has applied for " + targetName,
                "/university/applications",
                Map.of("applicationId", application.getId())
        );

        // Step 8: Log activity
        logActivity(userId, "APPLICATION_SUBMITTED",
                "Student applied to " + university.getName() + " (application #" + application.getId() + ")",
                "UniversityApplication", application.getId());

        // Step 9: Return response
        return toResponse(application, student, university, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL APPLICATION (student)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ApplicationResponse cancelApplication(Long applicationId, Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        UniversityApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found."));

        if (!application.getStudentId().equals(student.getId())) {
            throw new BadRequestException("You do not have permission to cancel this application.");
        }

        if (!"pending".equals(application.getStatus())) {
            throw new BadRequestException(
                    "Only pending applications can be cancelled (current status: " +
                    application.getStatus() + ").");
        }

        application.setStatus("cancelled");
        application.setCancelledAt(LocalDateTime.now());
        application = applicationRepository.save(application);

        // Notify university
        Institution university = institutionRepository.findById(application.getUniversityId()).orElse(null);
        if (university != null && university.getUser() != null) {
            String studentName = buildStudentName(student);
            notificationService.createNotification(
                    university.getUser().getId(),
                    "APPLICATION_WITHDRAWN",
                    "Application Withdrawn",
                    "Application withdrawn by student: " + studentName,
                    "/university/applications",
                    Map.of("applicationId", applicationId)
            );
        }

        logActivity(userId, "APPLICATION_CANCELLED",
                "Student cancelled application #" + applicationId,
                "UniversityApplication", applicationId);

        Student studentWithUser = studentRepository.findByUserIdWithUser(userId).orElse(student);
        return toResponse(application, studentWithUser, university, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET STUDENT APPLICATIONS (paginated)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getStudentApplications(
            Long userId, String status, Pageable pageable) {

        Student student = studentRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        String statusFilter = normalizeStatus(status, new String[]{"pending","accepted","rejected","cancelled"});

        return applicationRepository.findByStudentId(student.getId(), statusFilter, pageable)
                .map(app -> {
                    Institution university = institutionRepository
                            .findById(app.getUniversityId()).orElse(null);
                    return toResponse(app, student, university, false);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET UNIVERSITY APPLICATIONS (paginated — includes student certs)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getUniversityApplications(
            Long universityId, String status, Pageable pageable) {

        String statusFilter = normalizeStatus(status, new String[]{"pending","accepted","rejected"});

        Institution university = institutionRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found."));

        return applicationRepository.findByUniversityId(universityId, statusFilter, pageable)
                .map(app -> {
                    Student student = studentRepository.findById(app.getStudentId()).orElse(null);
                    // For list view, don't load certificates (keep it fast)
                    return toResponse(app, student, university, false);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET SINGLE APPLICATION (university view — includes student certs)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationDetail(Long applicationId, Long universityId) {
        UniversityApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found."));

        if (!application.getUniversityId().equals(universityId)) {
            throw new BadRequestException("This application does not belong to your institution.");
        }

        Institution university = institutionRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found."));

        Student student = studentRepository.findByIdWithUser(application.getStudentId()).orElse(null);

        // Include student certificates for the university to review
        ApplicationResponse response = toResponse(application, student, university, true);
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REVIEW APPLICATION (accept or reject)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ApplicationResponse reviewApplication(Long applicationId,
                                                  ReviewApplicationRequest dto,
                                                  Long universityId,
                                                  Long reviewedByUserId) {

        UniversityApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found."));

        if (!application.getUniversityId().equals(universityId)) {
            throw new BadRequestException("This application does not belong to your institution.");
        }

        if (!"pending".equals(application.getStatus())) {
            throw new BadRequestException(
                    "Only pending applications can be reviewed (current status: " +
                    application.getStatus() + ").");
        }

        Institution university = institutionRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found."));

        Student student = studentRepository.findByIdWithUser(application.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));

        application.setReviewedBy(reviewedByUserId);
        application.setReviewedAt(LocalDateTime.now());

        String activityAction;
        String notifType;
        String notifTitle;
        String notifMessage;

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // Accept
            application.setStatus("accepted");
            application.setAcceptanceMessage(dto.getMessage());

            activityAction = "APPLICATION_ACCEPTED";
            notifType      = "APPLICATION_ACCEPTED";
            notifTitle     = "Application Accepted!";
            notifMessage   = "Your application to " + university.getName() + " has been accepted! " +
                             "The university will contact you about enrollment.";
        } else {
            // Reject
            application.setStatus("rejected");
            application.setRejectionReason(dto.getMessage());

            activityAction = "APPLICATION_REJECTED";
            notifType      = "APPLICATION_REJECTED";
            notifTitle     = "Application Update";
            notifMessage   = "Your application to " + university.getName() +
                             " was not accepted this time.";
        }

        application = applicationRepository.save(application);

        // Notify student
        if (student.getUser() != null) {
            notificationService.createNotification(
                    student.getUser().getId(),
                    notifType,
                    notifTitle,
                    notifMessage,
                    "/student/applications",
                    Map.of("applicationId", applicationId)
            );
        }

        logActivity(reviewedByUserId, activityAction,
                activityAction + " application #" + applicationId + " for student #" + student.getId(),
                "UniversityApplication", applicationId);

        return toResponse(application, student, university, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET STUDENT HISTORY (comprehensive timeline)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentHistory(Long userId) {
        Student student = studentRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        Long studentId = student.getId();

        // ── 1. Applications ───────────────────────────────────────────────────
        List<Map<String, Object>> applications = applicationRepository
                .findByStudentId(studentId, null, Pageable.unpaged())
                .stream()
                .map(app -> {
                    Institution univ = institutionRepository.findById(app.getUniversityId()).orElse(null);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",               app.getId());
                    m.put("universityId",      app.getUniversityId());
                    m.put("universityName",    univ != null ? univ.getName() : null);
                    m.put("status",            app.getStatus());
                    m.put("appliedAt",         app.getAppliedAt());
                    m.put("reviewedAt",        app.getReviewedAt());
                    m.put("cancelledAt",       app.getCancelledAt());
                    m.put("programId",         app.getProgramId());
                    m.put("departmentId",      app.getDepartmentId());
                    m.put("certificateLevelId",app.getCertificateLevelId());
                    m.put("acceptanceMessage", app.getAcceptanceMessage());
                    m.put("rejectionReason",   app.getRejectionReason());
                    return m;
                })
                .collect(Collectors.toList());

        // ── 2. Enrollments ────────────────────────────────────────────────────
        List<Map<String, Object>> enrollments = enrollmentRepository
                .findByStudentId(studentId)
                .stream()
                .map(e -> {
                    Institution inst = institutionRepository.findById(e.getInstitutionId()).orElse(null);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",                   e.getId());
                    m.put("enrollmentNumber",      e.getEnrollmentNumber());
                    m.put("institutionId",         e.getInstitutionId());
                    m.put("institutionName",       inst != null ? inst.getName() : null);
                    m.put("status",                e.getStatus());
                    m.put("program",               e.getProgram());
                    m.put("batch",                 e.getBatch());
                    m.put("enrollmentDate",        e.getEnrollmentDate());
                    m.put("expectedGraduationDate",e.getExpectedGraduationDate());
                    m.put("actualGraduationDate",  e.getActualGraduationDate());
                    m.put("rollNumber",            e.getRollNumber());
                    m.put("createdAt",             e.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());

        // ── 3. Withdrawal requests ────────────────────────────────────────────
        List<Map<String, Object>> withdrawalRequests = withdrawalRequestRepository
                .findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(w -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",            w.getId());
                    m.put("enrollmentId",  w.getEnrollmentId());
                    m.put("status",        w.getStatus());
                    m.put("reason",        w.getReason());
                    m.put("rejectionNote", w.getRejectionNote());
                    m.put("reviewedAt",    w.getReviewedAt());
                    m.put("createdAt",     w.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());

        // ── 4. Certificates ───────────────────────────────────────────────────
        List<Map<String, Object>> certificates = certificateRepository
                .findByStudentIdOrderByIssueDateDesc(studentId)
                .stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",               c.getId());
                    m.put("serial",           c.getSerial());
                    m.put("certificateName",  c.getCertificateName());
                    m.put("certificateLevel", c.getCertificateLevel());
                    m.put("department",       c.getDepartment());
                    m.put("institutionId",    c.getInstitutionId());
                    m.put("institutionName",  c.getInstitution() != null ? c.getInstitution().getName() : null);
                    m.put("issueDate",        c.getIssueDate());
                    m.put("isRevoked",        c.isRevoked());
                    m.put("revokedAt",        c.getRevokedAt());
                    m.put("enrollmentId",     c.getEnrollmentId());
                    return m;
                })
                .collect(Collectors.toList());

        // ── 5. Access grants (given to verifiers) ─────────────────────────────
        List<Map<String, Object>> accessGrants = accessGrantRepository
                .findAllByStudentId(studentId)
                .stream()
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",         g.getId());
                    m.put("verifierId", g.getVerifierId());
                    m.put("grantedAt",  g.getGrantedAt());
                    m.put("expiresAt",  g.getExpiresAt());
                    m.put("revokedAt",  g.getRevokedAt());
                    m.put("isActive",   g.getRevokedAt() == null &&
                                       g.getExpiresAt() != null &&
                                       g.getExpiresAt().isAfter(LocalDateTime.now()));
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> history = new LinkedHashMap<>();
        history.put("applications",       applications);
        history.put("enrollments",        enrollments);
        history.put("withdrawalRequests", withdrawalRequests);
        history.put("certificates",       certificates);
        history.put("accessGrants",       accessGrants);

        return history;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACCEPTED-NOT-YET-ENROLLED (for "From Application" tab)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAcceptedNotYetEnrolled(Long universityId) {
        Institution university = institutionRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found."));

        return applicationRepository.findAcceptedNotYetEnrolled(universityId)
                .stream()
                .map(app -> {
                    Student student = studentRepository.findByIdWithUser(app.getStudentId()).orElse(null);
                    return toResponse(app, student, university, false);
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convert a {@link UniversityApplication} to an {@link ApplicationResponse}.
     *
     * @param includeStudentCerts if true, populates studentCertificates (university review view)
     */
    private ApplicationResponse toResponse(UniversityApplication app,
                                            Student student,
                                            Institution university,
                                            boolean includeStudentCerts) {
        ApplicationResponse r = new ApplicationResponse();
        r.setId(app.getId());
        r.setStatus(app.getStatus());
        r.setPersonalStatement(app.getPersonalStatement());
        r.setAppliedAt(app.getAppliedAt());
        r.setReviewedAt(app.getReviewedAt());
        r.setCancelledAt(app.getCancelledAt());
        r.setRejectionReason(app.getRejectionReason());
        r.setAcceptanceMessage(app.getAcceptanceMessage());

        // Student
        r.setStudentId(app.getStudentId());
        if (student != null) {
            r.setStudentName(buildStudentName(student));
            r.setStudentEmail(student.getUser() != null ? student.getUser().getEmail() : null);
        }

        // University
        r.setUniversityId(app.getUniversityId());
        if (university != null) {
            r.setUniversityName(university.getName());
        }

        // Program structure names
        r.setCertificateLevelId(app.getCertificateLevelId());
        if (app.getCertificateLevelId() != null) {
            certificateLevelRepository.findById(app.getCertificateLevelId())
                    .ifPresent(cl -> r.setCertificateLevelName(cl.getName()));
        }

        r.setDepartmentId(app.getDepartmentId());
        if (app.getDepartmentId() != null) {
            departmentRepository.findById(app.getDepartmentId())
                    .ifPresent(d -> r.setDepartmentName(d.getName()));
        }

        r.setProgramId(app.getProgramId());
        if (app.getProgramId() != null) {
            programRepository.findById(app.getProgramId())
                    .ifPresent(p -> r.setProgramName(p.getName()));
        }

        // Student certificates (university review only)
        if (includeStudentCerts && student != null) {
            List<Map<String, Object>> certs = certificateRepository
                    .findByStudentIdOrderByIssueDateDesc(student.getId())
                    .stream()
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id",               c.getId());
                        m.put("serial",           c.getSerial());
                        m.put("certificateName",  c.getCertificateName());
                        m.put("certificateLevel", c.getCertificateLevel());
                        m.put("department",       c.getDepartment());
                        m.put("institutionName",  c.getInstitution() != null ? c.getInstitution().getName() : null);
                        m.put("issueDate",        c.getIssueDate());
                        m.put("isRevoked",        c.isRevoked());
                        return m;
                    })
                    .collect(Collectors.toList());
            r.setStudentCertificates(certs);
        }

        return r;
    }

    private String buildStudentName(Student s) {
        if (s == null) return "Unknown";
        String name = s.getFirstName() != null ? s.getFirstName() : "";
        if (s.getMiddleName() != null && !s.getMiddleName().isBlank()) {
            name += " " + s.getMiddleName();
        }
        if (s.getLastName() != null) name += " " + s.getLastName();
        return name.trim();
    }

    /**
     * Resolve a human-readable name for the notification message.
     * Prefers program name, falls back to certificate level name.
     */
    private String resolveTargetName(Long programId, Long certLevelId) {
        if (programId != null) {
            return programRepository.findById(programId)
                    .map(p -> p.getShortName() != null ? p.getShortName() : p.getName())
                    .orElse("a program");
        }
        if (certLevelId != null) {
            return certificateLevelRepository.findById(certLevelId)
                    .map(CertificateLevel::getName)
                    .orElse("a certificate level");
        }
        return "admission";
    }

    /**
     * Normalize the status query param — returns null if "all" or unrecognized.
     */
    private String normalizeStatus(String status, String[] valid) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) return null;
        for (String v : valid) {
            if (v.equalsIgnoreCase(status)) return v.toLowerCase();
        }
        return null;
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
        } catch (Exception ex) {
            log.warn("Failed to log activity [{}]: {}", action, ex.getMessage());
        }
    }

}

