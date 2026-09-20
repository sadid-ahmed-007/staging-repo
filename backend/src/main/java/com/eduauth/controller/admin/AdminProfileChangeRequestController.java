package com.eduauth.controller.admin;

import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import com.eduauth.repository.specification.ProfileChangeRequestSpecification;
import com.eduauth.service.NotificationService;
import com.eduauth.util.HashUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin/profile-change-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProfileChangeRequestController {

    private final ProfileChangeRequestRepository profileChangeRequestRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstitutionRepository institutionRepository;
    private final VerifierRepository verifierRepository;
    private final CertificateRepository certificateRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15", name = "per_page") int perPage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(name = "field_name", required = false) String fieldName) {

        int pageIndex = Math.max(0, page - 1);
        var spec = ProfileChangeRequestSpecification.withFilters(status, role, fieldName);
        Page<ProfileChangeRequest> paged = profileChangeRequestRepository.findAll(spec, PageRequest.of(pageIndex, perPage));

        List<Map<String, Object>> requests = paged.getContent().stream()
                .map(this::formatRequestForAdmin)
                .toList();

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("current_page", paged.getNumber() + 1);
        pagination.put("last_page", Math.max(1, paged.getTotalPages()));
        pagination.put("per_page", perPage);
        pagination.put("total", paged.getTotalElements());

        long pendingCount = profileChangeRequestRepository.countByStatus("pending");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("requests", requests);
        response.put("pagination", pagination);
        response.put("pending_count", pendingCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {
        ProfileChangeRequest changeRequest = profileChangeRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile change request not found with id: " + id));

        Map<String, Object> formatted = formatRequestForAdmin(changeRequest);

        // Name fields impact for students
        List<String> nameFields = List.of("first_name", "middle_name", "last_name");
        if (changeRequest.getUser() != null
                && "student".equalsIgnoreCase(changeRequest.getUser().getRole())
                && changeRequest.getUser().getStudent() != null
                && nameFields.contains(changeRequest.getFieldName())) {
            long certCount = certificateRepository.countByStudentIdAndRevokedAtIsNull(changeRequest.getUser().getStudent().getId());
            formatted.put("certificate_count", certCount);
        }

        // Supporting documents
        List<Map<String, Object>> documents = parseDocuments(changeRequest);
        formatted.put("documents", documents);

        return ResponseEntity.ok(Map.of("success", true, "request", formatted));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approveRequest(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        ProfileChangeRequest changeRequest = profileChangeRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile change request not found with id: " + id));

        if (!changeRequest.isPending()) {
            return ResponseEntity.status(422).body(Map.of(
                    "success", false,
                    "message", "This request has already been reviewed."
            ));
        }

        User targetUser = changeRequest.getUser();
        String reviewNotes = (body != null && body.get("review_notes") != null) ? body.get("review_notes").toString().trim() : null;

        // 1. Apply the profile change
        applyChange(targetUser, changeRequest.getFieldName(), changeRequest.getRequestedValue());

        // 2. Mark request as approved
        changeRequest.setStatus("approved");
        changeRequest.setReviewer(admin);
        changeRequest.setReviewedAt(LocalDateTime.now());
        changeRequest.setReviewNotes(reviewNotes);
        profileChangeRequestRepository.save(changeRequest);

        // 3. Activity log for admin
        ActivityLog adminLog = new ActivityLog();
        adminLog.setUserId(admin.getId());
        adminLog.setAction("PROFILE_CHANGE_APPROVED");
        adminLog.setDescription(String.format("Approved profile change request #%d for user #%d (%s)",
                changeRequest.getId(), targetUser.getId(), changeRequest.getFieldName()));
        adminLog.setIpAddress(request.getRemoteAddr());
        activityLogRepository.save(adminLog);

        // 4. Activity log for user
        ActivityLog userLog = new ActivityLog();
        userLog.setUserId(targetUser.getId());
        userLog.setAction("PROFILE_CHANGE_APPROVED");
        userLog.setDescription(String.format("Your profile change request for %s has been approved.", changeRequest.getFieldName()));
        userLog.setIpAddress(request.getRemoteAddr());
        activityLogRepository.save(userLog);

        // 5. Name change certificate impact log
        List<String> nameFields = List.of("first_name", "middle_name", "last_name");
        if (nameFields.contains(changeRequest.getFieldName())
                && "student".equalsIgnoreCase(targetUser.getRole())
                && targetUser.getStudent() != null) {
            long certCount = certificateRepository.countByStudentIdAndRevokedAtIsNull(targetUser.getStudent().getId());
            if (certCount > 0) {
                ActivityLog certLog = new ActivityLog();
                certLog.setUserId(admin.getId());
                certLog.setAction("NAME_CHANGE_CERTIFICATE_IMPACT");
                certLog.setDescription(String.format("Student #%d has %d certificate(s) whose displayed name will now reflect the new legal name ('%s') on next download.",
                        targetUser.getId(), certCount, changeRequest.getRequestedValue()));
                certLog.setIpAddress(request.getRemoteAddr());
                activityLogRepository.save(certLog);
            }
        }

        // 6. User notification
        notificationService.createNotification(
                targetUser.getId(),
                "PROFILE_CHANGE",
                "Profile Change Approved",
                "Your profile change request for " + fieldLabel(changeRequest.getFieldName()) + " has been approved.",
                "/profile",
                Map.of("request_id", changeRequest.getId(), "field", changeRequest.getFieldName())
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Change request approved and applied successfully."
        ));
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectRequest(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        ProfileChangeRequest changeRequest = profileChangeRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile change request not found with id: " + id));

        if (!changeRequest.isPending()) {
            return ResponseEntity.status(422).body(Map.of(
                    "success", false,
                    "message", "This request has already been reviewed."
            ));
        }

        String reviewNotes = (body != null && body.get("review_notes") != null) ? body.get("review_notes").toString().trim() : "";
        if (reviewNotes.length() < 5) {
            return ResponseEntity.status(422).body(Map.of(
                    "success", false,
                    "message", "Please provide a valid reason for rejection (at least 5 characters)."
            ));
        }

        User targetUser = changeRequest.getUser();

        changeRequest.setStatus("rejected");
        changeRequest.setReviewer(admin);
        changeRequest.setReviewedAt(LocalDateTime.now());
        changeRequest.setReviewNotes(reviewNotes);
        profileChangeRequestRepository.save(changeRequest);

        // Activity log for admin
        ActivityLog adminLog = new ActivityLog();
        adminLog.setUserId(admin.getId());
        adminLog.setAction("PROFILE_CHANGE_REJECTED");
        adminLog.setDescription(String.format("Rejected profile change request #%d for user #%d (%s)",
                changeRequest.getId(), targetUser.getId(), changeRequest.getFieldName()));
        adminLog.setIpAddress(request.getRemoteAddr());
        activityLogRepository.save(adminLog);

        // Activity log for user
        ActivityLog userLog = new ActivityLog();
        userLog.setUserId(targetUser.getId());
        userLog.setAction("PROFILE_CHANGE_REJECTED");
        userLog.setDescription(String.format("Your profile change request for %s has been rejected.", changeRequest.getFieldName()));
        userLog.setIpAddress(request.getRemoteAddr());
        activityLogRepository.save(userLog);

        // User notification
        notificationService.createNotification(
                targetUser.getId(),
                "PROFILE_CHANGE",
                "Profile Change Rejected",
                "Your profile change request for " + fieldLabel(changeRequest.getFieldName()) + " was rejected. Reason: " + reviewNotes,
                "/profile",
                Map.of("request_id", changeRequest.getId(), "field", changeRequest.getFieldName(), "reason", reviewNotes)
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Change request has been rejected."
        ));
    }

    @GetMapping("/{id}/documents/{index}")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id, @PathVariable int index) {
        ProfileChangeRequest changeRequest = profileChangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile change request not found with id: " + id));

        List<String> docPaths = parseRawDocumentPaths(changeRequest.getSupportingDocumentPath());
        if (index < 0 || index >= docPaths.size()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Document not found."));
        }

        String relativePath = docPaths.get(index);
        File file = resolveDocumentFile(relativePath);

        if (file == null || !file.exists() || !file.isFile()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Document file not found."));
        }

        Resource resource = new FileSystemResource(file);
        String filename = file.getName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    private void applyChange(User user, String fieldName, String value) {
        switch (fieldName) {
            case "email" -> {
                user.setEmail(value);
                userRepository.save(user);
            }
            case "first_name" -> {
                if (user.getStudent() != null) {
                    user.getStudent().setFirstName(value);
                    studentRepository.save(user.getStudent());
                }
            }
            case "middle_name" -> {
                if (user.getStudent() != null) {
                    user.getStudent().setMiddleName(value);
                    studentRepository.save(user.getStudent());
                }
            }
            case "last_name" -> {
                if (user.getStudent() != null) {
                    user.getStudent().setLastName(value);
                    studentRepository.save(user.getStudent());
                }
            }
            case "date_of_birth" -> {
                if (user.getStudent() != null) {
                    user.getStudent().setDateOfBirth(LocalDate.parse(value));
                    studentRepository.save(user.getStudent());
                }
            }
            case "phone" -> {
                if (user.getStudent() != null) {
                    user.getStudent().setPhone(value);
                    studentRepository.save(user.getStudent());
                } else if (user.getInstitution() != null) {
                    user.getInstitution().setPhone(value);
                    institutionRepository.save(user.getInstitution());
                } else if (user.getVerifier() != null) {
                    user.getVerifier().setPhone(value);
                    verifierRepository.save(user.getVerifier());
                }
            }
            case "address" -> {
                if (user.getStudent() != null) {
                    user.getStudent().setAddress(value);
                    studentRepository.save(user.getStudent());
                } else if (user.getInstitution() != null) {
                    user.getInstitution().setAddress(value);
                    institutionRepository.save(user.getInstitution());
                }
            }
            case "nid" -> {
                if (user.getStudent() != null) {
                    user.getStudent().setNidHash(HashUtil.sha256(value));
                    studentRepository.save(user.getStudent());
                }
            }
            case "name" -> {
                if (user.getInstitution() != null) {
                    user.getInstitution().setName(value);
                    institutionRepository.save(user.getInstitution());
                }
            }
            case "registration_number" -> {
                if (user.getInstitution() != null) {
                    user.getInstitution().setRegistrationNumber(value);
                    institutionRepository.save(user.getInstitution());
                }
            }
            case "company_name" -> {
                if (user.getVerifier() != null) {
                    user.getVerifier().setCompanyName(value);
                    verifierRepository.save(user.getVerifier());
                }
            }
            case "website" -> {
                if (user.getInstitution() != null) {
                    user.getInstitution().setWebsite(value);
                    institutionRepository.save(user.getInstitution());
                } else if (user.getVerifier() != null) {
                    user.getVerifier().setWebsite(value);
                    verifierRepository.save(user.getVerifier());
                }
            }
            default -> {
            }
        }
    }

    private Map<String, Object> formatRequestForAdmin(ProfileChangeRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", req.getId());

        User user = req.getUser();
        map.put("user_id", user != null ? user.getId() : null);
        map.put("user_email", user != null ? user.getEmail() : null);
        map.put("user_role", user != null ? user.getRole() : null);
        map.put("user_name", resolveUserName(user));

        map.put("field_name", req.getFieldName());
        map.put("field_label", fieldLabel(req.getFieldName()));
        map.put("current_value", req.getCurrentValue());
        map.put("requested_value", req.getRequestedValue());
        map.put("reason", req.getReason());

        List<String> docs = parseRawDocumentPaths(req.getSupportingDocumentPath());
        map.put("has_documents", !docs.isEmpty());
        map.put("document_count", docs.size());

        map.put("status", req.getStatus());
        map.put("review_notes", req.getReviewNotes());
        map.put("reviewer_email", req.getReviewer() != null ? req.getReviewer().getEmail() : null);
        map.put("created_at", req.getCreatedAt() != null ? req.getCreatedAt().format(DATE_TIME_FORMATTER) : null);
        map.put("updated_at", req.getUpdatedAt() != null ? req.getUpdatedAt().format(DATE_TIME_FORMATTER) : null);

        // Include certificate count for student name-change requests
        List<String> nameFields = List.of("first_name", "middle_name", "last_name");
        if (user != null && "student".equalsIgnoreCase(user.getRole())
                && user.getStudent() != null && nameFields.contains(req.getFieldName())) {
            map.put("certificate_count", certificateRepository.countByStudentIdAndRevokedAtIsNull(user.getStudent().getId()));
        } else {
            map.put("certificate_count", 0);
        }

        return map;
    }

    private List<Map<String, Object>> parseDocuments(ProfileChangeRequest req) {
        List<String> paths = parseRawDocumentPaths(req.getSupportingDocumentPath());
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            String filename = new File(p).getName();
            list.add(Map.of(
                    "index", i,
                    "filename", filename,
                    "url", "/api/admin/profile-change-requests/" + req.getId() + "/documents/" + i
            ));
        }
        return list;
    }

    private List<String> parseRawDocumentPaths(String path) {
        if (path == null || path.isBlank()) return Collections.emptyList();
        // If it looks like JSON array, parse it (for backwards compatibility), else return as single item list
        if (path.trim().startsWith("[")) {
            try {
                return objectMapper.readValue(path, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        return Collections.singletonList(path);
    }

    private File resolveDocumentFile(String relativePath) {
        List<Path> candidates = List.of(
                Paths.get("uploads", relativePath),
                Paths.get("storage", relativePath),
                Paths.get("storage", "app", relativePath),
                Paths.get("storage", "app", "private", relativePath),
                Paths.get("storage", "app", "public", relativePath),
                Paths.get(relativePath)
        );

        for (Path p : candidates) {
            File f = p.toFile();
            if (f.exists() && f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private String resolveUserName(User user) {
        if (user == null) return "Unknown";
        if ("student".equalsIgnoreCase(user.getRole()) && user.getStudent() != null) {
            String fn = user.getStudent().getFirstName() != null ? user.getStudent().getFirstName() : "";
            String ln = user.getStudent().getLastName() != null ? user.getStudent().getLastName() : "";
            return (fn + " " + ln).trim();
        } else if ("university".equalsIgnoreCase(user.getRole()) && user.getInstitution() != null) {
            return user.getInstitution().getName() != null ? user.getInstitution().getName() : user.getEmail();
        } else if ("verifier".equalsIgnoreCase(user.getRole()) && user.getVerifier() != null) {
            return user.getVerifier().getCompanyName() != null ? user.getVerifier().getCompanyName() : user.getEmail();
        }
        return user.getEmail();
    }

    private String fieldLabel(String fieldName) {
        if (fieldName == null) return "";
        return switch (fieldName) {
            case "email" -> "Email Address";
            case "first_name" -> "First Name";
            case "middle_name" -> "Middle Name";
            case "last_name" -> "Last Name";
            case "date_of_birth" -> "Date of Birth";
            case "nid" -> "NID / Birth Certificate";
            case "phone" -> "Phone Number";
            case "address" -> "Address";
            case "name" -> "Institution Name";
            case "registration_number" -> "Registration Number";
            case "company_name" -> "Company Name";
            case "website" -> "Company Link / Website";
            default -> fieldName;
        };
    }
}
