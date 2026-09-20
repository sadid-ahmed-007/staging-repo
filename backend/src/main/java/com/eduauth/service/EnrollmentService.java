package com.eduauth.service;

import com.eduauth.dto.enrollment.*;
import com.eduauth.exception.BadRequestException;
import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstitutionRepository institutionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CertificateSequenceRepository certificateSequenceRepository;
    private final CertificateLevelRepository certificateLevelRepository;
    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;
    private final ProgramRepository programRepository;
    private final NotificationService notificationService;
    private final CertificateRepository certificateRepository;
    private final com.eduauth.repository.UniversityApplicationRepository universityApplicationRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // ENROLL STUDENT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EnrollmentResponse enrollStudent(EnrollRequest request,
                                            Long institutionId,
                                            Long enrolledByUserId) {
        // Step 1: Find student by email
        User studentUser = userRepository.findByEmail(request.getStudentEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + request.getStudentEmail()));

        if (!"student".equalsIgnoreCase(studentUser.getRole())) {
            throw new BadRequestException("The provided email does not belong to a student account.");
        }

        if (!Boolean.TRUE.equals(studentUser.getIsApproved())) {
            throw new BadRequestException("This student account has not been approved by admin yet.");
        }

        Student student = studentRepository.findByUserId(studentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        // Step 2: One active enrollment rule — across ALL universities
        // Check if student has any active enrollment anywhere
        boolean hasActiveGlobally = enrollmentRepository.existsByStudentIdAndStatus(student.getId(), "active");

        // Also check if student has a pending withdrawal at any institution
        // (active enrollment with a pending withdrawal request)
        boolean hasPendingWithdrawal = false;
        if (!hasActiveGlobally) {
            List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentId(student.getId())
                    .stream()
                    .filter(e -> "active".equals(e.getStatus()))
                    .collect(Collectors.toList());
            for (Enrollment ae : activeEnrollments) {
                if (withdrawalRequestRepository.existsByEnrollmentIdAndStatus(ae.getId(), "pending")) {
                    hasPendingWithdrawal = true;
                    break;
                }
            }
        }

        if (hasActiveGlobally || hasPendingWithdrawal) {
            throw new BadRequestException(
                    "This student is currently enrolled at another institution. " +
                    "They cannot be enrolled again until their current enrollment " +
                    "is graduated or withdrawn.");
        }

        // Step 3: Check if already enrolled at THIS institution with non-withdrawn status
        List<Enrollment> existingAtThisInstitution =
                enrollmentRepository.findByStudentIdAndInstitutionId(student.getId(), institutionId);

        for (Enrollment existing : existingAtThisInstitution) {
            if (!"withdrawn".equals(existing.getStatus()) && !"graduated".equals(existing.getStatus())) {
                throw new BadRequestException(
                        "This student is already enrolled at your institution (status: "
                        + existing.getStatus() + "). " +
                        "A student can only be re-enrolled if their previous enrollment was withdrawn or graduated.");
            }
        }

        // Check if roll_number is already in use at this institution by another student
        if (request.getStudentIdInUniversity() != null && !request.getStudentIdInUniversity().isBlank()) {
            enrollmentRepository.findByInstitutionId(institutionId).stream()
                    .filter(e -> request.getStudentIdInUniversity().equalsIgnoreCase(e.getRollNumber())
                            && !e.getStudentId().equals(student.getId()))
                    .findFirst()
                    .ifPresent(e -> {
                        throw new BadRequestException(
                                "The Student ID '" + request.getStudentIdInUniversity() +
                                "' is already assigned to another student at your institution.");
                    });
        }

        // Step 4: Resolve program (if programId provided) and validate/calculate dates
        LocalDate enrollDate = request.getEnrollmentDate();
        LocalDate gradDate = request.getExpectedGraduationDate();

        // ── Program resolution ────────────────────────────────────────────────
        Program resolvedProgram = null;
        Department resolvedDept = null;
        CertificateLevel resolvedLevel = null;

        if (request.getProgramId() != null) {
            resolvedProgram = programRepository.findById(request.getProgramId()).orElse(null);
            if (resolvedProgram != null) {
                resolvedDept = departmentRepository.findById(resolvedProgram.getDepartmentId()).orElse(null);
                if (resolvedDept != null && resolvedDept.getCertificateLevelId() != null) {
                    resolvedLevel = certificateLevelRepository.findById(resolvedDept.getCertificateLevelId()).orElse(null);
                }
            }
        }

        // Auto-calculate graduation date from program duration if not explicitly provided
        if (gradDate == null) {
            if (resolvedLevel != null && resolvedLevel.getDurationYears() != null) {
                gradDate = enrollDate.plusYears(resolvedLevel.getDurationYears());
            } else {
                throw new BadRequestException(
                        "Expected graduation date is required when no programId is provided.");
            }
        }

        if (enrollDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Enrollment date cannot be in the future.");
        }
        if (!gradDate.isAfter(enrollDate)) {
            throw new BadRequestException("Expected graduation date must be after the enrollment date.");
        }

        // Step 5: Generate enrollment number inside the transaction
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found."));

        String enrollmentNumber = generateEnrollmentNumber(institution);

        // Step 6: Create enrollment record
        // If programId is provided, auto-derive names from the program hierarchy
        String programName  = resolvedProgram != null ? resolvedProgram.getName()  : request.getProgram();
        String departmentName = resolvedDept  != null ? resolvedDept.getName()     : request.getDepartment();
        String majorName    = request.getMajor();

        // Fall back to departmentId/majorId lookup for legacy text fields
        if (departmentName == null && request.getDepartmentId() != null) {
            departmentName = departmentRepository.findById(request.getDepartmentId())
                    .map(Department::getName).orElse(null);
        }
        if (majorName == null && request.getMajorId() != null) {
            majorName = majorRepository.findById(request.getMajorId())
                    .map(Major::getName).orElse(null);
        }

        // Determine IDs to store
        Long certLevelId = resolvedLevel  != null ? resolvedLevel.getId()  : request.getCertificateLevelId();
        Long deptId      = resolvedDept   != null ? resolvedDept.getId()   : request.getDepartmentId();
        Long majorId     = request.getMajorId();
        Long programId   = resolvedProgram != null ? resolvedProgram.getId() : null;

        // Encode program, department, major into single program column
        // Format: "program||department||major" (department and major may be empty)
        String encodedProgram = encodeProgram(programName, departmentName, majorName);

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentNumber(enrollmentNumber);
        enrollment.setStudentId(student.getId());
        enrollment.setInstitutionId(institutionId);
        enrollment.setRollNumber(request.getStudentIdInUniversity());
        enrollment.setProgram(encodedProgram);
        enrollment.setBatch(request.getBatch());
        enrollment.setStatus("active");
        enrollment.setEnrollmentDate(enrollDate);
        enrollment.setExpectedGraduationDate(gradDate);
        enrollment.setEnrolledBy(enrolledByUserId);
        enrollment.setCertificateLevelId(certLevelId);
        enrollment.setDepartmentId(deptId);
        enrollment.setMajorId(majorId);
        enrollment.setProgramId(programId);

        enrollment = enrollmentRepository.save(enrollment);

        // Log activity
        logActivity(enrolledByUserId, "STUDENT_ENROLLED",
                "Enrolled student " + studentUser.getEmail() + " in " + request.getProgram() +
                " at institution #" + institutionId,
                "Enrollment", enrollment.getId());

        // Notify student: ENROLLMENT_CONFIRMED
        final Long studentUserId = studentUser.getId();
        final Long enrollmentId  = enrollment.getId();
        notificationService.createNotification(
                studentUserId,
                "ENROLLMENT_CONFIRMED",
                "Enrollment Confirmed",
                "Your enrollment has been confirmed" +
                        (institution != null ? " at " + institution.getName() : "") + ".",
                "/student/enrollment",
                Map.of("enrollmentId", enrollmentId)
        );

        return toResponse(enrollment, studentUser, institution, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ENROLLMENTS (paginated, for university)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollments(Long institutionId,
                                                    String status,
                                                    String search,
                                                    Pageable pageable) {
        String statusFilter = (status == null || status.isBlank() || "all".equalsIgnoreCase(status))
                ? null : status;
        String searchFilter = (search == null || search.isBlank()) ? null : search;

        // When filtering by withdrawal_requested: we look at active enrollments
        // that have a pending withdrawal request
        if ("withdrawal_requested".equals(statusFilter)) {
            // Return from the in-memory computed list (small enough for UI)
            List<Enrollment> withPending =
                    enrollmentRepository.findWithPendingWithdrawalByInstitutionId(institutionId);
            // Build institution for context
            Institution institution = institutionRepository.findById(institutionId).orElse(null);
            List<EnrollmentResponse> responses = withPending.stream()
                    .map(e -> {
                        User u = getStudentUser(e.getStudentId());
                        WithdrawalRequest wr = withdrawalRequestRepository
                                .findFirstByEnrollmentIdAndStatusOrderByCreatedAtDesc(e.getId(), "pending")
                                .orElse(null);
                        EnrollmentResponse r = toResponse(e, u, institution, wr);
                        r.setStatus("withdrawal_requested");
                        return r;
                    })
                    .collect(Collectors.toList());
            // Wrap into a Page manually
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), responses.size());
            List<EnrollmentResponse> pageContent = (start > responses.size())
                    ? List.of() : responses.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, responses.size());
        }

        Page<Enrollment> page = enrollmentRepository.findByInstitutionWithFilters(
                institutionId, statusFilter, searchFilter, pageable);

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        return page.map(e -> {
            User u = getStudentUser(e.getStudentId());
            // Check for pending withdrawal
            WithdrawalRequest wr = null;
            boolean hasPending = withdrawalRequestRepository
                    .existsByEnrollmentIdAndStatus(e.getId(), "pending");
            if (hasPending) {
                wr = withdrawalRequestRepository
                        .findFirstByEnrollmentIdAndStatusOrderByCreatedAtDesc(e.getId(), "pending")
                        .orElse(null);
            }
            EnrollmentResponse resp = toResponse(e, u, institution, wr);
            if (hasPending && "active".equals(e.getStatus())) {
                resp.setStatus("withdrawal_requested");
            }
            return resp;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET SINGLE ENROLLMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long id, Long institutionId) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        if (!enrollment.getInstitutionId().equals(institutionId)) {
            throw new BadRequestException("This enrollment does not belong to your institution.");
        }

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        User studentUser = getStudentUser(enrollment.getStudentId());

        // For the university detail view, fetch any withdrawal request (pending, approved, or rejected)
        WithdrawalRequest wr = withdrawalRequestRepository
                .findFirstByEnrollmentIdOrderByCreatedAtDesc(id)
                .orElse(null);

        EnrollmentResponse resp = toResponse(enrollment, studentUser, institution, wr);
        // Synthesise the withdrawal_requested virtual status when pending WR exists
        if (wr != null && "pending".equals(wr.getStatus()) && "active".equals(enrollment.getStatus())) {
            resp.setStatus("withdrawal_requested");
        }
        return resp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EDIT ENROLLMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EnrollmentResponse editEnrollment(Long id, EditEnrollmentRequest request, Long institutionId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        if (!enrollment.getInstitutionId().equals(institutionId)) {
            throw new BadRequestException("This enrollment does not belong to your institution.");
        }

        if ("graduated".equals(enrollment.getStatus()) || "withdrawn".equals(enrollment.getStatus())) {
            throw new BadRequestException(
                    "Cannot edit a " + enrollment.getStatus() + " enrollment.");
        }

        // Only update provided (non-null) fields
        // Parse existing encoded program to get current parts
        String[] existing = decodeProgram(enrollment.getProgram());
        String newProgram    = request.getProgram()    != null ? request.getProgram()    : existing[0];
        String newDepartment = request.getDepartment() != null ? request.getDepartment() : existing[1];
        if (request.getDepartmentId() != null) {
            newDepartment = departmentRepository.findById(request.getDepartmentId()).map(Department::getName).orElse(newDepartment);
            enrollment.setDepartmentId(request.getDepartmentId());
        }

        String newMajor      = request.getMajor()      != null ? request.getMajor()      : existing[2];
        if (request.getMajorId() != null) {
            newMajor = majorRepository.findById(request.getMajorId()).map(Major::getName).orElse(newMajor);
            enrollment.setMajorId(request.getMajorId());
        }

        if (request.getCertificateLevelId() != null) {
            enrollment.setCertificateLevelId(request.getCertificateLevelId());
        }

        enrollment.setProgram(encodeProgram(newProgram, newDepartment, newMajor));
        if (request.getBatch() != null) enrollment.setBatch(request.getBatch());
        if (request.getExpectedGraduationDate() != null) {
            enrollment.setExpectedGraduationDate(request.getExpectedGraduationDate());
        }

        enrollment = enrollmentRepository.save(enrollment);

        logActivity(userId, "ENROLLMENT_UPDATED",
                "Updated enrollment details for enrollment #" + id,
                "Enrollment", id);

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        User studentUser = getStudentUser(enrollment.getStudentId());
        return toResponse(enrollment, studentUser, institution, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXTEND GRADUATION DATE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EnrollmentResponse extendGraduation(Long id, ExtendGraduationRequest request,
                                               Long institutionId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        if (!enrollment.getInstitutionId().equals(institutionId)) {
            throw new BadRequestException("This enrollment does not belong to your institution.");
        }

        if (!"active".equals(enrollment.getStatus())) {
            throw new BadRequestException(
                    "Can only extend graduation date for active enrollments.");
        }

        LocalDate newDate = request.getNewExpectedGraduationDate();

        if (!newDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("New graduation date must be in the future.");
        }

        if (enrollment.getExpectedGraduationDate() != null
                && !newDate.isAfter(enrollment.getExpectedGraduationDate())) {
            throw new BadRequestException(
                    "New graduation date must be after the current expected graduation date ("
                    + enrollment.getExpectedGraduationDate() + ").");
        }

        LocalDate oldDate = enrollment.getExpectedGraduationDate();
        enrollment.setExpectedGraduationDate(newDate);
        enrollment = enrollmentRepository.save(enrollment);

        logActivity(userId, "GRADUATION_DATE_EXTENDED",
                "Extended graduation date for enrollment #" + id +
                " from " + oldDate + " to " + newDate +
                ". Reason: " + request.getReason(),
                "Enrollment", id);

        // Notify student: GRADUATION_EXTENDED
        Student studentForNotif = studentRepository.findById(enrollment.getStudentId()).orElse(null);
        if (studentForNotif != null && studentForNotif.getUser() != null) {
            notificationService.createNotification(
                    studentForNotif.getUser().getId(),
                    "GRADUATION_EXTENDED",
                    "Graduation Date Extended",
                    "Your expected graduation date has been extended to " + newDate + ".",
                    "/student/enrollment",
                    Map.of("enrollmentId", id, "newGraduationDate", newDate.toString())
            );
        }

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        User studentUser = getStudentUser(enrollment.getStudentId());
        return toResponse(enrollment, studentUser, institution, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT: REQUEST WITHDRAWAL
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EnrollmentResponse requestWithdrawal(Long enrollmentId, WithdrawalRequestDto request,
                                                Long studentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        // Verify the student owns this enrollment
        Student student = studentRepository.findByUserId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        if (!enrollment.getStudentId().equals(student.getId())) {
            throw new BadRequestException("You do not have permission to withdraw from this enrollment.");
        }

        if (!"active".equals(enrollment.getStatus())) {
            throw new BadRequestException(
                    "Can only request withdrawal from an active enrollment. " +
                    "Current status: " + enrollment.getStatus());
        }

        // Check if a pending withdrawal already exists
        if (withdrawalRequestRepository.existsByEnrollmentIdAndStatus(enrollmentId, "pending")) {
            throw new BadRequestException(
                    "A withdrawal request is already pending for this enrollment.");
        }

        // Create the withdrawal request (enrollment status stays 'active' in DB)
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setEnrollmentId(enrollmentId);
        wr.setStudentId(student.getId());
        wr.setReason(request.getReason());
        wr.setStatus("pending");
        wr = withdrawalRequestRepository.save(wr);

        logActivity(studentId, "WITHDRAWAL_REQUESTED",
                "Student requested withdrawal from enrollment #" + enrollmentId,
                "Enrollment", enrollmentId);

        Institution institution = institutionRepository.findById(enrollment.getInstitutionId()).orElse(null);
        User studentUser = userRepository.findById(studentId).orElse(null);
        EnrollmentResponse resp = toResponse(enrollment, studentUser, institution, wr);
        resp.setStatus("withdrawal_requested");
        return resp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT: CANCEL WITHDRAWAL REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelWithdrawalRequest(Long enrollmentId, Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found."));

        WithdrawalRequest wr = null;
        if (enrollmentId != null) {
            wr = withdrawalRequestRepository
                    .findFirstByEnrollmentIdAndStatusOrderByCreatedAtDesc(enrollmentId, "pending")
                    .orElse(null);
        }
        if (wr == null) {
            wr = withdrawalRequestRepository
                    .findFirstByStudentIdAndStatusOrderByCreatedAtDesc(student.getId(), "pending")
                    .orElseThrow(() -> new BadRequestException("No pending withdrawal request found to cancel."));
        }

        if (!wr.getStudentId().equals(student.getId())) {
            throw new BadRequestException("You do not have permission to cancel this withdrawal request.");
        }

        Long targetEnrollmentId = wr.getEnrollmentId();
        withdrawalRequestRepository.delete(wr);

        logActivity(userId, "WITHDRAWAL_CANCELLED",
                "Student cancelled pending withdrawal request for enrollment #" + targetEnrollmentId,
                "Enrollment", targetEnrollmentId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNIVERSITY: RESPOND TO WITHDRAWAL REQUEST
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EnrollmentResponse respondToWithdrawal(Long enrollmentId,
                                                   WithdrawalResponseRequest request,
                                                   Long institutionId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        if (!enrollment.getInstitutionId().equals(institutionId)) {
            throw new BadRequestException("This enrollment does not belong to your institution.");
        }

        WithdrawalRequest wr = withdrawalRequestRepository
                .findFirstByEnrollmentIdAndStatusOrderByCreatedAtDesc(enrollmentId, "pending")
                .orElseThrow(() -> new BadRequestException(
                        "No pending withdrawal request found for this enrollment."));

        wr.setReviewedBy(userId);
        wr.setReviewedAt(LocalDateTime.now());

        String action;
        if (request.isApproved()) {
            wr.setStatus("approved");
            enrollment.setStatus("withdrawn");
            enrollment.setActualGraduationDate(LocalDate.now());
            action = "WITHDRAWAL_APPROVED";
        } else {
            wr.setStatus("rejected");
            wr.setRejectionNote(request.getResponseMessage());
            // status stays active
            action = "WITHDRAWAL_REJECTED";
        }

        enrollmentRepository.save(enrollment);
        withdrawalRequestRepository.save(wr);

        logActivity(userId, action,
                "Responded to withdrawal request for enrollment #" + enrollmentId +
                ": " + (request.isApproved() ? "approved" : "rejected") +
                " — " + request.getResponseMessage(),
                "Enrollment", enrollmentId);

        // Notify student: WITHDRAWAL_APPROVED or WITHDRAWAL_REJECTED
        Student studentForNotif = studentRepository.findById(enrollment.getStudentId()).orElse(null);
        if (studentForNotif != null && studentForNotif.getUser() != null) {
            if (request.isApproved()) {
                notificationService.createNotification(
                        studentForNotif.getUser().getId(),
                        "WITHDRAWAL_APPROVED",
                        "Withdrawal Approved",
                        "Your withdrawal request has been approved.",
                        "/student/enrollment",
                        Map.of("enrollmentId", enrollmentId)
                );
            } else {
                notificationService.createNotification(
                        studentForNotif.getUser().getId(),
                        "WITHDRAWAL_REJECTED",
                        "Withdrawal Rejected",
                        "Your withdrawal request has been rejected" +
                                (request.getResponseMessage() != null && !request.getResponseMessage().isBlank()
                                        ? ": " + request.getResponseMessage() : "."),
                        "/student/enrollment",
                        Map.of("enrollmentId", enrollmentId)
                );
            }
        }

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        User studentUser = getStudentUser(enrollment.getStudentId());
        return toResponse(enrollment, studentUser, institution,
                request.isApproved() ? null : wr);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNIVERSITY: DIRECT WITHDRAWAL (without student request)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public EnrollmentResponse directWithdraw(Long enrollmentId, String reason,
                                              Long institutionId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found."));

        if (!enrollment.getInstitutionId().equals(institutionId)) {
            throw new BadRequestException("This enrollment does not belong to your institution.");
        }

        if (!"active".equals(enrollment.getStatus())) {
            throw new BadRequestException(
                    "Can only directly withdraw an active enrollment. " +
                    "Current status: " + enrollment.getStatus());
        }

        enrollment.setStatus("withdrawn");
        enrollment.setActualGraduationDate(LocalDate.now());
        enrollment = enrollmentRepository.save(enrollment);

        logActivity(userId, "STUDENT_WITHDRAWN",
                "Directly withdrew student from enrollment #" + enrollmentId +
                ". Reason: " + reason,
                "Enrollment", enrollmentId);

        // Notify student: WITHDRAWAL_REQUESTED (university-initiated direct withdrawal)
        Student studentForNotif = studentRepository.findById(enrollment.getStudentId()).orElse(null);
        if (studentForNotif != null && studentForNotif.getUser() != null) {
            notificationService.createNotification(
                    studentForNotif.getUser().getId(),
                    "WITHDRAWAL_REQUESTED",
                    "Withdrawal Processed",
                    "Your enrollment has been withdrawn by the university" +
                            (reason != null && !reason.isBlank() ? ": " + reason : "."),
                    "/student/enrollment",
                    Map.of("enrollmentId", enrollmentId)
            );
        }

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        User studentUser = getStudentUser(enrollment.getStudentId());
        return toResponse(enrollment, studentUser, institution, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT: GET CURRENT ENROLLMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EnrollmentResponse getStudentCurrentEnrollment(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElse(null);
        if (student == null) return null;

        // Find active enrollment
        Enrollment enrollment = enrollmentRepository
                .findActiveByStudentId(student.getId())
                .orElse(null);

        if (enrollment == null) return null;

        Institution institution = institutionRepository
                .findById(enrollment.getInstitutionId()).orElse(null);
        User studentUser = userRepository.findById(userId).orElse(null);

        // Check for pending withdrawal
        WithdrawalRequest wr = withdrawalRequestRepository
                .findFirstByEnrollmentIdAndStatusOrderByCreatedAtDesc(enrollment.getId(), "pending")
                .orElse(null);

        EnrollmentResponse resp = toResponse(enrollment, studentUser, institution, wr);
        if (wr != null) {
            resp.setStatus("withdrawal_requested");
        }
        return resp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT: WITHDRAWAL STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getWithdrawalStatus(Long userId) {
        Student student = studentRepository.findByUserId(userId).orElse(null);
        if (student == null) {
            return Map.of("success", false, "message", "Student profile not found");
        }

        Enrollment currentEnrollment = enrollmentRepository
                .findActiveByStudentId(student.getId())
                .orElse(null);

        if (currentEnrollment == null) {
            return Map.of(
                    "success", true,
                    "data", Map.of("hasRequest", false)
            );
        }

        WithdrawalRequest wr = withdrawalRequestRepository
                .findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .filter(w -> w.getEnrollmentId().equals(currentEnrollment.getId()))
                .findFirst()
                .orElse(null);

        if (wr == null) {
            return Map.of(
                    "success", true,
                    "data", Map.of("hasRequest", false)
            );
        }

        Enrollment enrollment = enrollmentRepository
                .findById(wr.getEnrollmentId()).orElse(null);

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("hasRequest", true);
        data.put("status", wr.getStatus());
        data.put("reason", wr.getReason());
        data.put("rejectionNote", wr.getRejectionNote() != null ? wr.getRejectionNote() : "");
        data.put("createdAt", wr.getCreatedAt());
        data.put("reviewedAt", wr.getReviewedAt());
        data.put("enrollmentId", wr.getEnrollmentId());
        data.put("program", enrollment != null ? enrollment.getProgram() : "");
        data.put("institutionId", enrollment != null ? enrollment.getInstitutionId() : null);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNIVERSITY: LIST PENDING WITHDRAWAL REQUESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getPendingWithdrawalRequests(Long institutionId) {
        List<Enrollment> enrollments =
                enrollmentRepository.findWithPendingWithdrawalByInstitutionId(institutionId);

        Institution institution = institutionRepository.findById(institutionId).orElse(null);

        return enrollments.stream().map(e -> {
            User u = getStudentUser(e.getStudentId());
            WithdrawalRequest wr = withdrawalRequestRepository
                    .findFirstByEnrollmentIdAndStatusOrderByCreatedAtDesc(e.getId(), "pending")
                    .orElse(null);
            EnrollmentResponse resp = toResponse(e, u, institution, wr);
            resp.setStatus("withdrawal_requested");
            return resp;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL: AUTO-GRADUATE (called by CertificateService)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called internally when a certificate is issued.
     * Must be called within the SAME @Transactional context as certificate creation.
     */
    public void autoGraduate(Long enrollmentId, LocalDate graduationDate) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found for auto-graduation: " + enrollmentId));

        enrollment.setStatus("graduated");
        enrollment.setActualGraduationDate(graduationDate != null ? graduationDate : LocalDate.now());
        enrollmentRepository.save(enrollment);

        log.info("Auto-graduated enrollment #{} on {}", enrollmentId, graduation(enrollment));
    }

    private LocalDate graduation(Enrollment e) {
        return e.getActualGraduationDate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEARCH STUDENTS TO ENROLL
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchStudentsToEnroll(String q, Long institutionId) {
        if (q == null || q.trim().length() < 2) {
            throw new BadRequestException("Search query must be at least 2 characters.");
        }

        List<Student> students = studentRepository.searchApprovedStudents(q.trim());

        return students.stream().map(student -> {
            User u = student.getUser();
            String email = u != null ? u.getEmail() : "";

            // Check current enrollment status
            boolean isActiveAnywhere = enrollmentRepository
                    .existsByStudentIdAndStatus(student.getId(), "active");

            boolean hasPendingWithdrawal = false;
            String activeInstitution = null;

            List<Enrollment> allEnrollments = enrollmentRepository.findByStudentId(student.getId());
            for (Enrollment e : allEnrollments) {
                if ("active".equals(e.getStatus())) {
                    if (withdrawalRequestRepository.existsByEnrollmentIdAndStatus(e.getId(), "pending")) {
                        hasPendingWithdrawal = true;
                    }
                    Institution inst = institutionRepository.findById(e.getInstitutionId()).orElse(null);
                    activeInstitution = inst != null ? inst.getName() : "Unknown";
                    break;
                }
            }

            String currentStatus = "not_enrolled";
            if (isActiveAnywhere && hasPendingWithdrawal) currentStatus = "withdrawal_requested";
            else if (isActiveAnywhere) currentStatus = "active";

            // Check if this student has an accepted application to THIS institution
            boolean hasAcceptedApplication = universityApplicationRepository
                    .existsByStudentIdAndUniversityIdAndStatus(student.getId(), institutionId, "accepted");

            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", student.getId());
            row.put("name", buildName(student));
            row.put("email", email);
            row.put("currentEnrollmentStatus", currentStatus);
            row.put("activeInstitution", activeInstitution != null ? activeInstitution : "");
            row.put("canEnroll", !isActiveAnywhere && !hasPendingWithdrawal);
            row.put("hasAcceptedApplication", hasAcceptedApplication);
            return row;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEARCH ENROLLED STUDENTS (for Certificate Issuance)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchEnrolledStudents(String q, Long institutionId) {
        String searchTerm = (q != null) ? q.trim() : "";

        List<Enrollment> enrollments = enrollmentRepository.searchEnrolledStudents(
                institutionId, searchTerm, PageRequest.of(0, 30));

        return enrollments.stream().map(e -> {
            Student student = e.getStudent();
            User u = (student != null) ? student.getUser() : null;
            String studentName = (student != null) ? buildName(student) : "N/A";
            String email = (u != null) ? u.getEmail() : "";

            String[] programParts = decodeProgram(e.getProgram());
            String program = programParts[0];
            String department = programParts[1];
            String major = programParts[2];

            String certLevelName = null;
            if (e.getCertificateLevelId() != null) {
                certLevelName = certificateLevelRepository.findById(e.getCertificateLevelId())
                        .map(CertificateLevel::getName).orElse(null);
            }

            if (e.getProgramId() != null) {
                Program p = programRepository.findById(e.getProgramId()).orElse(null);
                if (p != null) {
                    program = p.getName();
                    if ((department == null || department.isBlank()) && p.getDepartmentId() != null) {
                        Department d = departmentRepository.findById(p.getDepartmentId()).orElse(null);
                        if (d != null) {
                            department = d.getName();
                            if (certLevelName == null && d.getCertificateLevelId() != null) {
                                certLevelName = certificateLevelRepository.findById(d.getCertificateLevelId())
                                        .map(CertificateLevel::getName).orElse(null);
                            }
                        }
                    }
                }
            } else if (e.getDepartmentId() != null && (department == null || department.isBlank())) {
                department = departmentRepository.findById(e.getDepartmentId()).map(Department::getName).orElse("");
            }

            Map<String, Object> enrollmentInfo = new java.util.LinkedHashMap<>();
            enrollmentInfo.put("id", e.getId());
            enrollmentInfo.put("enrollmentNumber", e.getEnrollmentNumber());
            enrollmentInfo.put("enrollment_number", e.getEnrollmentNumber());
            enrollmentInfo.put("rollNumber", e.getRollNumber());
            enrollmentInfo.put("roll_number", e.getRollNumber());
            enrollmentInfo.put("program", program);
            enrollmentInfo.put("programName", program);
            enrollmentInfo.put("certificateName", program);
            enrollmentInfo.put("certificateLevel", certLevelName);
            enrollmentInfo.put("certificateLevelName", certLevelName);
            enrollmentInfo.put("department", department);
            enrollmentInfo.put("major", major);
            enrollmentInfo.put("status", e.getStatus());
            enrollmentInfo.put("session", e.getBatch());
            enrollmentInfo.put("expectedGraduationDate", e.getExpectedGraduationDate());
            enrollmentInfo.put("expected_graduation_date", e.getExpectedGraduationDate());

            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", student != null ? student.getId() : null);
            row.put("studentId", student != null ? student.getId() : null);
            row.put("name", studentName);
            row.put("studentName", studentName);
            row.put("email", email);
            row.put("enrollmentNumber", e.getEnrollmentNumber());
            row.put("enrollment_number", e.getEnrollmentNumber());
            row.put("rollNumber", e.getRollNumber());
            row.put("roll_number", e.getRollNumber());
            row.put("studentIdInUniversity", e.getRollNumber());
            row.put("program", program);
            row.put("programName", program);
            row.put("certificateName", program);
            row.put("certificateLevel", certLevelName);
            row.put("certificateLevelName", certLevelName);
            row.put("department", department);
            row.put("major", major);
            row.put("session", e.getBatch());
            row.put("status", e.getStatus());
            row.put("expectedGraduationDate", e.getExpectedGraduationDate());
            row.put("expected_graduation_date", e.getExpectedGraduationDate());
            row.put("enrollmentId", e.getId());
            row.put("programId", e.getProgramId());
            row.put("certificateLevelId", e.getCertificateLevelId());
            row.put("departmentId", e.getDepartmentId());
            row.put("enrollments", List.of(enrollmentInfo));

            return row;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENROLLMENT NUMBER GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Format: {INST_PREFIX}-{YEAR}-{SEQUENCE}
     * Example: UIU-26-000001
     * Uses pessimistic locking on certificate_sequences for uniqueness under concurrency.
     */
    @Transactional
    public String generateEnrollmentNumber(Institution institution) {
        String instPrefix = "ENR";
        if (institution != null && institution.getRegistrationNumber() != null && institution.getRegistrationNumber().contains("-")) {
            instPrefix = institution.getRegistrationNumber().split("-")[0].trim().toUpperCase();
        } else if (institution != null && institution.getName() != null && !institution.getName().isBlank()) {
            String[] words = institution.getName().trim().split("\\s+");
            if (words.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (String w : words) {
                    if (!w.isBlank() && Character.isLetterOrDigit(w.charAt(0))) {
                        sb.append(Character.toUpperCase(w.charAt(0)));
                    }
                }
                instPrefix = sb.toString();
            } else {
                instPrefix = institution.getName().substring(0, Math.min(4, institution.getName().length())).toUpperCase();
            }
        }
        if (instPrefix.length() > 6) {
            instPrefix = instPrefix.substring(0, 6);
        }

        String year = String.valueOf(LocalDate.now().getYear()).substring(2); // 2-digit year suffix: e.g. "26"
        Long instId = institution != null ? institution.getId() : 0L;
        String seqPrefix = (instPrefix + "_" + instId);
        if (seqPrefix.length() > 20) {
            seqPrefix = seqPrefix.substring(0, 20);
        }

        final String finalSeqPrefix = seqPrefix;
        CertificateSequence seq = certificateSequenceRepository
                .findByPrefixAndYearSuffixWithLock(finalSeqPrefix, year)
                .orElseGet(() -> {
                    CertificateSequence newSeq = new CertificateSequence();
                    newSeq.setPrefix(finalSeqPrefix);
                    newSeq.setYearSuffix(year);
                    newSeq.setSequenceKey("enrollment_serial_" + instId);
                    newSeq.setCurrentSequence(0L);
                    return newSeq;
                });

        long nextSeq = seq.getCurrentSequence() + 1;
        String candidateNumber = instPrefix + "-" + year + "-" + String.format("%06d", nextSeq);
        while (enrollmentRepository.existsByEnrollmentNumber(candidateNumber)) {
            nextSeq++;
            candidateNumber = instPrefix + "-" + year + "-" + String.format("%06d", nextSeq);
        }

        seq.setCurrentSequence(nextSeq);
        seq.setLastGeneratedAt(LocalDateTime.now());
        certificateSequenceRepository.saveAndFlush(seq);

        return candidateNumber;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STAT HELPERS (used by controller)
    // ─────────────────────────────────────────────────────────────────────────

    public long countByStatus(Long institutionId, String status) {
        return enrollmentRepository.countByInstitutionIdAndStatus(institutionId, status);
    }

    public long countAll(Long institutionId) {
        return enrollmentRepository.countByInstitutionId(institutionId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private EnrollmentResponse toResponse(Enrollment e, User studentUser,
                                           Institution institution,
                                           WithdrawalRequest wr) {
        Student student = e.getStudent();
        if (student == null && e.getStudentId() != null) {
            student = studentRepository.findById(e.getStudentId()).orElse(null);
        }
        String studentName = "N/A";
        if (student != null) {
            studentName = buildName(student);
        }

        // Decode program||department||major from the combined program column
        String[] programParts = decodeProgram(e.getProgram());
        String deptName = programParts[1];
        String majName = programParts[2];
        
        if ((deptName == null || deptName.isBlank()) && e.getDepartmentId() != null) {
            deptName = departmentRepository.findById(e.getDepartmentId()).map(Department::getName).orElse("");
        }
        if ((majName == null || majName.isBlank()) && e.getMajorId() != null) {
            majName = majorRepository.findById(e.getMajorId()).map(Major::getName).orElse("");
        }

        EnrollmentResponse.EnrollmentResponseBuilder builder = EnrollmentResponse.builder()
                .id(e.getId())
                .enrollmentNumber(e.getEnrollmentNumber())
                .studentId(e.getStudentId())
                .studentName(studentName)
                .studentEmail(studentUser != null ? studentUser.getEmail() : null)
                .studentIdInUniversity(e.getRollNumber())
                .program(programParts[0])
                .department(deptName)
                .major(majName)
                .certificateLevelId(e.getCertificateLevelId())
                .departmentId(e.getDepartmentId())
                .majorId(e.getMajorId())
                .programId(e.getProgramId())
                .batch(e.getBatch())
                .status(e.getStatus())
                .enrollmentDate(e.getEnrollmentDate())
                .expectedGraduationDate(e.getExpectedGraduationDate())
                .actualGraduationDate(e.getActualGraduationDate())
                .institutionName(institution != null ? institution.getName() : null)
                .createdAt(e.getCreatedAt());

        // Populate programName / programShortName from the programs table if programId is set
        if (e.getProgramId() != null) {
            programRepository.findById(e.getProgramId()).ifPresent(p -> {
                builder.programName(p.getName());
                builder.programShortName(p.getShortName());
            });
        }

        if (wr != null) {
            builder.withdrawalReason(wr.getReason())
                   .withdrawalRequestedAt(wr.getCreatedAt());

            // Build the full WithdrawalRequestInfo for the university view
            WithdrawalRequestInfo wrInfo = WithdrawalRequestInfo.builder()
                    .requestedBy("student")        // WR records are always student-initiated
                    .reason(wr.getReason())
                    .requestedAt(wr.getCreatedAt())
                    .responseMessage(wr.getRejectionNote())
                    .respondedAt(wr.getReviewedAt())
                    .status(wr.getStatus())
                    .build();
            builder.withdrawalRequest(wrInfo);
        }

        if ("graduated".equals(e.getStatus())) {
            certificateRepository.findFirstByEnrollmentIdOrderByIssueDateDesc(e.getId()).ifPresent(cert -> {
                builder.certificateId(cert.getId())
                       .certificateSerial(cert.getSerial());
            });
        }

        return builder.build();
    }

    private User getStudentUser(Long studentId) {
        if (studentId == null) return null;
        return studentRepository.findById(studentId)
                .map(s -> s.getUser())
                .orElse(null);
    }

    private String buildName(Student s) {
        StringBuilder sb = new StringBuilder();
        if (s.getFirstName() != null) sb.append(s.getFirstName());
        if (s.getMiddleName() != null && !s.getMiddleName().isBlank()) {
            sb.append(" ").append(s.getMiddleName());
        }
        if (s.getLastName() != null) sb.append(" ").append(s.getLastName());
        return sb.toString().trim();
    }

    /**
     * Encodes program, department, and major into a single string stored in the `program` column.
     * Format: "program||department||major"
     * This avoids needing additional DB columns while still allowing the API to return
     * all three fields separately.
     */
    private String encodeProgram(String program, String department, String major) {
        String p = program    != null ? program.trim()    : "";
        String d = department != null ? department.trim() : "";
        String m = major      != null ? major.trim()      : "";
        String encoded = p + "||" + d + "||" + m;
        return encoded.length() > 255 ? encoded.substring(0, 255) : encoded;
    }

    /**
     * Decodes a program string back into [program, department, major].
     * Returns [original, "", ""] if not in encoded format.
     */
    private String[] decodeProgram(String encoded) {
        if (encoded == null) return new String[]{"", "", ""};
        String[] parts = encoded.split("\\|\\|", -1);
        if (parts.length == 3) return parts;
        if (parts.length == 2) return new String[]{parts[0], parts[1], ""};
        return new String[]{encoded, "", ""};  // Legacy: no encoding
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
            // Don't fail the main operation if logging fails
            this.log.warn("Activity logging failed for action {}: {}", action, ex.getMessage());
        }
    }
}

