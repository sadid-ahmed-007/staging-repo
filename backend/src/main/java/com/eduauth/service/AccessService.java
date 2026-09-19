package com.eduauth.service;

import com.eduauth.dto.access.AccessGrantListDto;
import com.eduauth.dto.access.AccessRequestListDto;
import com.eduauth.dto.access.RespondToAccessRequestDto;
import com.eduauth.dto.access.SendAccessRequestDto;
import com.eduauth.exception.BadRequestException;
import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles all access request and access grant business logic:
 *  - Verifier sends access request to student
 *  - Student approves/rejects, creating an AccessGrant on approval
 *  - Verifier or student can list, cancel, or revoke
 *  - checkAccess() is the gatekeeper for all verifier certificate reads
 *
 * Rate-limiting note: In production this search endpoint should be rate-limited
 * to 20 requests/minute per verifier (not implemented here — handled at API gateway).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessService {

    private final AccessRequestRepository accessRequestRepository;
    private final AccessGrantRepository   accessGrantRepository;
    private final StudentRepository       studentRepository;
    private final VerifierRepository      verifierRepository;
    private final ActivityLogRepository   activityLogRepository;
    private final NotificationService     notificationService;
    private final CertificateRepository   certificateRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFIER SIDE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifier sends an access request to a student.
     *
     * Steps:
     *  1. Resolve and validate student
     *  2. Guard against duplicate pending request
     *  3. Guard against already-active access grant
     *  4. Create AccessRequest record
     *  5. Notify student
     *  6. Log activity
     */
    @Transactional
    public AccessRequest sendAccessRequest(SendAccessRequestDto dto, Long verifierId) {
        // Step 1 — Resolve student
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (student.getUser() == null
                || !Boolean.TRUE.equals(student.getUser().getIsApproved())
                || student.getUser().getSuspendedAt() != null) {
            throw new BadRequestException("Student account is not available");
        }

        // Determine target certificate
        Long targetCertificateId = null;
        Certificate targetCert = null;
        if (!Boolean.TRUE.equals(dto.getRequestAllCertificates()) && dto.getCertificateId() != null) {
            targetCert = certificateRepository.findById(dto.getCertificateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
            if (!targetCert.getStudentId().equals(student.getId())) {
                throw new BadRequestException("Certificate does not belong to this student");
            }
            targetCertificateId = targetCert.getId();
        }

        LocalDateTime now = LocalDateTime.now();

        // Step 2 — Guard against duplicate pending request
        if (targetCertificateId == null) {
            if (accessRequestRepository.existsPendingRequestForAll(verifierId, dto.getStudentId())) {
                throw new BadRequestException("You already have a pending access request for all certificates of this student");
            }
        } else {
            if (accessRequestRepository.existsPendingRequestForCertificate(verifierId, dto.getStudentId(), targetCertificateId)) {
                throw new BadRequestException("You already have a pending access request for this certificate");
            }
            if (accessRequestRepository.existsPendingRequestForAll(verifierId, dto.getStudentId())) {
                throw new BadRequestException("You already have a pending access request for all certificates of this student");
            }
        }

        // Step 3 — Guard against already-active grant
        if (targetCertificateId == null) {
            if (accessGrantRepository.existsActiveGrantForAllCertificates(verifierId, dto.getStudentId(), now)) {
                throw new BadRequestException("You already have active access to all certificates of this student");
            }
        } else {
            if (!accessGrantRepository.findActiveGrantsForCertificate(verifierId, dto.getStudentId(), targetCertificateId, now).isEmpty()) {
                throw new BadRequestException("You already have active access to this certificate");
            }
        }

        // Step 4 — Create AccessRequest
        AccessRequest req = new AccessRequest();
        req.setVerifierId(verifierId);
        req.setStudentId(dto.getStudentId());
        req.setCertificateId(targetCertificateId);
        req.setPurpose(dto.getPurpose());
        req.setRequestedDurationDays(dto.getRequestedDurationDays());
        req.setStatus("pending");
        req = accessRequestRepository.save(req);

        // Step 5 — Notify student
        Verifier verifier = verifierRepository.findById(verifierId).orElse(null);
        String companyName = verifier != null ? verifier.getCompanyName() : "A verifier";
        Long studentUserId = student.getUser().getId();

        String notificationMessage = targetCert != null
                ? companyName + " has requested access to your certificate (" + targetCert.getCertificateName() + " - " + targetCert.getSerial() + ")"
                : companyName + " has requested access to all your certificates";

        createNotification(
                studentUserId,
                "ACCESS_REQUEST",
                "New Access Request",
                notificationMessage,
                Map.of("accessRequestId", req.getId()),
                "/student/access-requests"
        );

        // Step 6 — Log activity
        logActivity(verifierId, "ACCESS_REQUEST_SENT", "AccessRequest", req.getId(),
                "Sent access request to student " + dto.getStudentId()
                        + (targetCert != null ? " for certificate " + targetCert.getSerial() : " for all certificates"));

        return req;
    }

    /**
     * Get paginated list of access requests sent by a verifier.
     * status = "all" or null → return all (including cancelled/soft-deleted)
     * status = "cancelled"   → return only soft-deleted rows
     * status = "pending" | "approved" | "rejected" → filter by status (non-deleted only)
     */
    public Page<AccessRequest> getVerifierRequests(Long verifierId, String status, Pageable pageable) {
        if (status == null || "all".equalsIgnoreCase(status)) {
            return accessRequestRepository.findByVerifierIdOrderByRequestedAtDesc(verifierId, pageable);
        }
        if ("cancelled".equalsIgnoreCase(status)) {
            // No status for cancelled; return all and filter in service
            // For now return empty page — cancellations are shown in "all"
            return Page.empty(pageable);
        }
        return accessRequestRepository.findByVerifierIdAndStatusActive(verifierId, status, pageable);
    }

    /**
     * Cancel a pending access request (verifier only).
     * Soft-deletes the row (sets deleted_at = now).
     */
    @Transactional
    public void cancelAccessRequest(Long requestId, Long verifierId) {
        AccessRequest req = accessRequestRepository
                .findPendingByIdAndVerifierId(requestId, verifierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pending access request not found or cannot be cancelled"));

        req.setDeletedAt(LocalDateTime.now());
        accessRequestRepository.save(req);

        logActivity(verifierId, "ACCESS_REQUEST_CANCELLED", "AccessRequest", req.getId(),
                "Cancelled pending access request for student " + req.getStudentId());
    }

    /**
     * Get paginated list of requests received by a student.
     * status = "all" or null → all non-deleted requests
     * status = "pending" | "approved" | "rejected" → filtered
     */
    public Page<AccessRequest> getStudentRequests(Long studentId, String status, Pageable pageable) {
        String statusFilter = ("all".equalsIgnoreCase(status) || status == null) ? null : status;
        return accessRequestRepository.findByStudentIdAndStatus(studentId, statusFilter, pageable);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT SIDE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Student responds to an access request — either approves or rejects.
     *
     * On approval: creates an AccessGrant and notifies the verifier.
     * On rejection: updates status to 'rejected' and notifies the verifier.
     */
    @Transactional
    public AccessRequest respondToRequest(Long requestId, RespondToAccessRequestDto dto, Long studentId) {
        // Step 1 — Find the request belonging to this student
        AccessRequest req = accessRequestRepository
                .findPendingByIdAndStudentId(requestId, studentId)
                .orElse(null);

        if (req == null) {
            // Idempotency check: if already processed, return current state without error
            AccessRequest existing = accessRequestRepository.findById(requestId).orElse(null);
            if (existing != null && studentId.equals(existing.getStudentId()) && !"pending".equals(existing.getStatus())) {
                return existing;
            }
            throw new ResourceNotFoundException("Pending access request not found");
        }

        LocalDateTime now = LocalDateTime.now();

        // Resolve student for notification message
        Student student = studentRepository.findById(studentId).orElse(null);
        String studentName = student != null
                ? (student.getFirstName() + " " + student.getLastName()).trim()
                : "Student";

        // Resolve verifier for notification target
        Verifier verifier = verifierRepository.findById(req.getVerifierId()).orElse(null);
        Long verifierUserId = (verifier != null && verifier.getUser() != null)
                ? verifier.getUser().getId() : null;

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // Cross-field validation
            if (dto.getDurationDays() == null) {
                throw new BadRequestException("Duration days is required when approving an access request");
            }

            // Update request
            req.setStatus("approved");
            req.setRespondedAt(now);
            req.setResponseMessage(dto.getResponseMessage());
            req.setRequestedDurationDays(dto.getDurationDays()); // store approved duration
            accessRequestRepository.save(req);

            // Create access grant
            AccessGrant grant = new AccessGrant();
            grant.setAccessRequestId(req.getId());
            grant.setVerifierId(req.getVerifierId());
            grant.setStudentId(studentId);
            grant.setCertificateId(req.getCertificateId()); // carries over certificate scope!
            grant.setGrantedAt(now);
            grant.setExpiresAt(now.plusDays(dto.getDurationDays()));
            accessGrantRepository.save(grant);

            // If access to ALL certificates is granted, any existing active single-certificate grants
            // for this verifier and student are superseded by the universal grant.
            if (req.getCertificateId() == null) {
                List<AccessGrant> priorGrants = accessGrantRepository
                        .findAllActiveGrantsForVerifierAndStudent(req.getVerifierId(), studentId, now);
                for (AccessGrant prior : priorGrants) {
                    if (prior.getCertificateId() != null && !prior.getId().equals(grant.getId())) {
                        prior.setRevokedAt(now);
                        prior.setRevokedBy(student != null && student.getUser() != null ? student.getUser().getId() : null);
                        accessGrantRepository.save(prior);
                    }
                }
            }

            // Notify verifier
            if (verifierUserId != null) {
                String expiryDate = grant.getExpiresAt()
                        .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                String notifMsg;
                if (req.getCertificateId() != null) {
                    Certificate cert = certificateRepository.findById(req.getCertificateId()).orElse(null);
                    String certDesc = cert != null ? (" (" + cert.getCertificateName() + ")") : "";
                    notifMsg = "Your access request for certificate" + certDesc + " was approved. Access expires on " + expiryDate;
                } else {
                    notifMsg = "Your access request for all certificates was approved. Access expires on " + expiryDate;
                }
                createNotification(
                        verifierUserId,
                        "ACCESS_REQUEST_APPROVED",
                        "Access Request Approved",
                        notifMsg,
                        Map.of("accessRequestId", req.getId(), "accessGrantId", grant.getId()),
                        "/verifier/accessible-certificates"
                );
            }

            logActivity(studentId, "ACCESS_REQUEST_APPROVED", "AccessRequest", req.getId(),
                    studentName + " approved access request from verifier " + req.getVerifierId()
                            + " for " + dto.getDurationDays() + " days");

        } else {
            // Rejected — reason must be at least 10 characters
            if (dto.getResponseMessage() == null || dto.getResponseMessage().trim().length() < 10) {
                throw new BadRequestException("Rejection reason must be at least 10 characters");
            }
            req.setStatus("rejected");
            req.setRespondedAt(now);
            req.setResponseMessage(dto.getResponseMessage().trim());
            accessRequestRepository.save(req);

            // Notify verifier
            if (verifierUserId != null) {
                createNotification(
                        verifierUserId,
                        "ACCESS_REQUEST_REJECTED",
                        "Access Request Rejected",
                        "Your access request was rejected",
                        Map.of("accessRequestId", req.getId()),
                        "/verifier/access-requests"
                );
            }

            logActivity(studentId, "ACCESS_REQUEST_REJECTED", "AccessRequest", req.getId(),
                    studentName + " rejected access request from verifier " + req.getVerifierId());
        }

        return req;
    }

    /**
     * Get all grants for a student — active grants + historical (expired/revoked).
     * Returns a map with "active" and "history" lists of AccessGrantListDto.
     */
    public Map<String, Object> getStudentGrants(Long studentId) {
        LocalDateTime now = LocalDateTime.now();

        List<AccessGrant> activeGrants = accessGrantRepository
                .findByStudentIdAndRevokedAtIsNullAndExpiresAtAfter(studentId, now);

        List<AccessGrant> allGrants = accessGrantRepository.findAllByStudentId(studentId);
        List<AccessGrant> historyGrants = allGrants.stream()
                .filter(g -> g.getRevokedAt() != null || !g.getExpiresAt().isAfter(now))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active",  activeGrants.stream().map(g -> toGrantDto(g, now)).collect(Collectors.toList()));
        result.put("history", historyGrants.stream().map(g -> toGrantDto(g, now)).collect(Collectors.toList()));
        return result;
    }

    /**
     * Student revokes an active access grant.
     */
    @Transactional
    public void revokeGrant(Long grantId, Long studentId) {
        // Security: load only if it belongs to this student and is not already revoked
        AccessGrant grant = accessGrantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Access grant not found"));

        if (!grant.getStudentId().equals(studentId)) {
            // Do not reveal that the grant exists for a different student
            throw new ResourceNotFoundException("Access grant not found");
        }

        if (grant.getRevokedAt() != null) {
            throw new BadRequestException("This grant has already been revoked");
        }

        LocalDateTime now = LocalDateTime.now();
        Student student = studentRepository.findById(studentId).orElse(null);
        String studentName = student != null
                ? (student.getFirstName() + " " + student.getLastName()).trim()
                : "Student";

        grant.setRevokedAt(now);
        grant.setRevokedBy(studentId); // student.id — using student pk as revokedBy
        accessGrantRepository.save(grant);

        // Notify verifier
        Verifier verifier = verifierRepository.findById(grant.getVerifierId()).orElse(null);
        if (verifier != null && verifier.getUser() != null) {
            createNotification(
                    verifier.getUser().getId(),
                    "ACCESS_REVOKED",
                    "Access Revoked",
                    "Your access to " + studentName + "'s certificates has been revoked",
                    Map.of("accessGrantId", grant.getId()),
                    "/verifier/access-requests"
            );
        }

        logActivity(studentId, "ACCESS_REVOKED", "AccessGrant", grant.getId(),
                studentName + " revoked access grant for verifier " + grant.getVerifierId());
    }

    /**
     * Admin revokes any access grant in the system.
     * Sets revokedAt = now, notifies both student and verifier, and logs ADMIN_ACCESS_REVOKED.
     */
    @Transactional
    public void adminRevokeGrant(Long grantId, Long adminUserId) {
        AccessGrant grant = accessGrantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Access grant not found"));

        if (grant.getRevokedAt() != null) {
            throw new BadRequestException("This grant has already been revoked");
        }

        LocalDateTime now = LocalDateTime.now();
        grant.setRevokedAt(now);
        grant.setRevokedBy(adminUserId);
        accessGrantRepository.save(grant);

        Student student = studentRepository.findById(grant.getStudentId()).orElse(null);
        String studentName = student != null
                ? (student.getFirstName() + " " + student.getLastName()).trim()
                : "Student";

        Verifier verifier = verifierRepository.findById(grant.getVerifierId()).orElse(null);
        String verifierName = verifier != null
                ? (verifier.getCompanyName() != null ? verifier.getCompanyName() : verifier.getContactPerson())
                : "Verifier";

        // Notify verifier
        if (verifier != null && verifier.getUser() != null) {
            createNotification(
                    verifier.getUser().getId(),
                    "ADMIN_ACCESS_REVOKED",
                    "Access Revoked by Administrator",
                    "Your access to " + studentName + "'s certificates has been revoked by an administrator",
                    Map.of("accessGrantId", grant.getId()),
                    "/verifier/accessible-certificates"
            );
        }

        // Notify student
        if (student != null && student.getUser() != null) {
            createNotification(
                    student.getUser().getId(),
                    "ADMIN_ACCESS_REVOKED",
                    "Access Revoked by Administrator",
                    "Access granted to " + verifierName + " for your certificates has been revoked by an administrator",
                    Map.of("accessGrantId", grant.getId()),
                    "/student/access-requests"
            );
        }

        // Log activity: ADMIN_ACCESS_REVOKED
        logActivity(adminUserId, "ADMIN_ACCESS_REVOKED", "AccessGrant", grant.getId(),
                "Admin revoked access grant " + grant.getId() + " between student " + grant.getStudentId()
                        + " and verifier " + grant.getVerifierId());
    }

    /**
     * Admin queries all access grants system-wide with dynamic filters and pagination.
     */
    public Page<AccessGrant> getAdminGrants(Long studentId, Long verifierId, String status, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        Specification<AccessGrant> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }
            if (verifierId != null) {
                predicates.add(cb.equal(root.get("verifierId"), verifierId));
            }

            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                switch (status.toLowerCase()) {
                    case "active" -> {
                        predicates.add(cb.isNull(root.get("revokedAt")));
                        predicates.add(cb.greaterThan(root.get("expiresAt"), now));
                    }
                    case "expired" -> {
                        predicates.add(cb.isNull(root.get("revokedAt")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("expiresAt"), now));
                    }
                    case "revoked" -> {
                        predicates.add(cb.isNotNull(root.get("revokedAt")));
                    }
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return accessGrantRepository.findAll(spec, pageable);
    }

    /**
     * Get all students this verifier currently has active access to.
     * Returns a structured list including student details and certificate count.
     */
    public List<AccessGrant> getVerifierAccessibleStudents(Long verifierId) {
        LocalDateTime now = LocalDateTime.now();
        return accessGrantRepository.findByVerifierIdAndRevokedAtIsNullAndExpiresAtAfter(verifierId, now);
    }

    /**
     * Gatekeeper — called before returning any certificate data to a verifier.
     * Returns true ONLY if a non-revoked, non-expired grant exists.
     */
    public boolean checkAccess(Long verifierId, Long studentId) {
        return accessGrantRepository.existsByVerifierIdAndStudentIdAndRevokedAtIsNullAndExpiresAtAfter(
                verifierId, studentId, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO Mappers
    // ─────────────────────────────────────────────────────────────────────────

    public AccessRequestListDto toRequestDto(AccessRequest req) {
        AccessRequestListDto dto = new AccessRequestListDto();
        dto.setId(req.getId());
        dto.setPurpose(req.getPurpose());
        dto.setRequestedDurationDays(req.getRequestedDurationDays());
        // Soft-deleted rows → status = "cancelled"
        dto.setStatus(req.getDeletedAt() != null ? "cancelled" : req.getStatus());
        dto.setRequestedAt(req.getCreatedAt());
        dto.setRespondedAt(req.getRespondedAt());
        dto.setResponseMessage(req.getResponseMessage());

        // Populate verifier name/company/email
        if (req.getVerifierId() != null) {
            verifierRepository.findById(req.getVerifierId()).ifPresent(v -> {
                dto.setVerifierName(v.getContactPerson());
                dto.setVerifierCompany(v.getCompanyName());
                dto.setVerifierEmail(v.getEmail());
            });
        }

        // Populate student name
        if (req.getStudentId() != null) {
            dto.setStudentId(req.getStudentId());
            studentRepository.findById(req.getStudentId()).ifPresent(s -> {
                dto.setStudentName((s.getFirstName() + " " + s.getLastName()).trim());
            });
        }

        // Populate active access and expiration state
        if ("approved".equalsIgnoreCase(req.getStatus())) {
            Optional<AccessGrant> grantOpt = accessGrantRepository.findByAccessRequestId(req.getId());
            if (grantOpt.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                grantOpt = accessGrantRepository.findActiveGrant(
                        req.getVerifierId(), req.getStudentId(), now);
            }
            if (grantOpt.isPresent()) {
                boolean isActive = grantOpt.get().getRevokedAt() == null &&
                        (grantOpt.get().getExpiresAt() == null || grantOpt.get().getExpiresAt().isAfter(LocalDateTime.now()));
                dto.setHasActiveAccess(isActive);
                dto.setAccessExpiresAt(grantOpt.get().getExpiresAt());
            } else {
                dto.setHasActiveAccess(false);
                if (req.getRespondedAt() != null && req.getRequestedDurationDays() != null) {
                    dto.setAccessExpiresAt(req.getRespondedAt().plusDays(req.getRequestedDurationDays()));
                }
            }
        }

        // Populate certificate details if specific
        if (req.getCertificateId() != null) {
            dto.setCertificateId(req.getCertificateId());
            dto.setRequestScope("specific");
            certificateRepository.findById(req.getCertificateId()).ifPresent(c -> {
                dto.setCertificateSerial(c.getSerial());
                dto.setCertificateName(c.getCertificateName());
                dto.setCertificateLevel(c.getCertificateLevel());
            });
        } else {
            dto.setRequestScope("all");
        }

        return dto;
    }

    public AccessGrantListDto toGrantDto(AccessGrant grant, LocalDateTime now) {
        AccessGrantListDto dto = new AccessGrantListDto();
        dto.setId(grant.getId());
        dto.setGrantedAt(grant.getGrantedAt());
        dto.setExpiresAt(grant.getExpiresAt());
        dto.setRevokedAt(grant.getRevokedAt());

        boolean active = grant.getRevokedAt() == null
                && grant.getExpiresAt() != null
                && grant.getExpiresAt().isAfter(now);
        dto.setActive(active);

        long days = 0;
        if (active && grant.getExpiresAt() != null) {
            days = ChronoUnit.DAYS.between(now, grant.getExpiresAt());
            if (days < 0) days = 0;
        }
        dto.setDaysRemaining(days);

        // Populate verifier details
        if (grant.getVerifierId() != null) {
            dto.setVerifierId(grant.getVerifierId());
            verifierRepository.findById(grant.getVerifierId()).ifPresent(v -> {
                dto.setVerifierName(v.getContactPerson());
                dto.setVerifierCompany(v.getCompanyName());
                dto.setVerifierEmail(v.getEmail());
            });
        }

        // Populate student details
        if (grant.getStudentId() != null) {
            dto.setStudentId(grant.getStudentId());
            studentRepository.findByIdWithUser(grant.getStudentId()).ifPresent(s -> {
                dto.setStudentName((s.getFirstName() + " " + s.getLastName()).trim());
                if (s.getUser() != null) {
                    dto.setStudentEmail(s.getUser().getEmail());
                }
            });
        }

        // Populate certificate details if specific
        if (grant.getCertificateId() != null) {
            dto.setCertificateId(grant.getCertificateId());
            dto.setGrantScope("specific");
            certificateRepository.findById(grant.getCertificateId()).ifPresent(c -> {
                dto.setCertificateSerial(c.getSerial());
                dto.setCertificateName(c.getCertificateName());
                dto.setCertificateLevel(c.getCertificateLevel());
            });
        } else {
            dto.setGrantScope("all");
        }

        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Delegate notification creation to NotificationService.
     * Signature kept identical to callers above (data + actionUrl swapped from old impl).
     */
    private void createNotification(Long userId, String type, String title,
                                    String message, Map<String, Object> data, String actionUrl) {
        notificationService.createNotification(userId, type, title, message, actionUrl, data);
    }

    private void logActivity(Long userId, String action, String entityType,
                             Long entityId, String description) {
        try {
            ActivityLog log = new ActivityLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDescription(description);
            activityLogRepository.save(log);
        } catch (Exception ex) {
            // Activity log failure must not break main flow
        }
    }
}
