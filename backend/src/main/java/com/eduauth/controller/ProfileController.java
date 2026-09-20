package com.eduauth.controller;

import com.eduauth.dto.profile.PasswordUpdateRequest;
import com.eduauth.dto.profile.ProfileUpdateRequest;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import com.eduauth.service.JwtService;
import com.eduauth.service.TokenBlacklistService;
import com.eduauth.model.AccountDeletionRequest;
import com.eduauth.repository.AccountDeletionRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.eduauth.repository.ProfileChangeRequestRepository;
import java.util.Arrays;

import org.springframework.web.multipart.MultipartFile;
import com.eduauth.service.FileStorageService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private VerifierRepository verifierRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private TokenBlacklistService blacklistService;
    @Autowired private ActivityLogRepository activityLogRepository;
    @Autowired private ProfileChangeRequestRepository profileChangeRequestRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private AccountDeletionRequestRepository accountDeletionRequestRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("isApproved", user.getIsApproved());
        response.put("createdAt", user.getCreatedAt());
        response.put("lastLoginAt", null); // Simplification per requirement

        switch (user.getRole()) {
            case "student":
                Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                if (student != null) {
                    Map<String, Object> studentData = new HashMap<>();
                    studentData.put("firstName", student.getFirstName());
                    studentData.put("middleName", student.getMiddleName());
                    studentData.put("lastName", student.getLastName());
                    studentData.put("dateOfBirth", student.getDateOfBirth());
                    studentData.put("phone", student.getPhone());
                    studentData.put("address", student.getAddress());
                    
                    // NID display logic
                    String nidDisplay = "Not set";
                    if (student.getNidEncrypted() != null && !student.getNidEncrypted().isEmpty()) {
                        nidDisplay = "Set";
                    } else if (student.getNidHash() != null && !student.getNidHash().isEmpty()) {
                        nidDisplay = "Set";
                    }
                    studentData.put("nidHash", nidDisplay);
                    studentData.put("avatarUrl", student.getAvatarPath());
                    
                    // Enrollment logic to get studentId
                    Enrollment activeEnrollment = enrollmentRepository.findActiveByStudentId(student.getId()).orElse(null);
                    studentData.put("studentId", activeEnrollment != null && activeEnrollment.getRollNumber() != null ? activeEnrollment.getRollNumber() : "Not assigned");
                    response.put("student", studentData);

                    Map<String, Object> enrollmentData = null;
                    if (activeEnrollment != null) {
                        enrollmentData = new HashMap<>();
                        if (activeEnrollment.getInstitution() != null) {
                            enrollmentData.put("institutionName", activeEnrollment.getInstitution().getName());
                        }
                        enrollmentData.put("program", activeEnrollment.getProgram());
                        enrollmentData.put("batch", activeEnrollment.getBatch());
                        enrollmentData.put("status", activeEnrollment.getStatus());
                        enrollmentData.put("expectedGraduationDate", activeEnrollment.getExpectedGraduationDate());
                    }
                    response.put("currentEnrollment", enrollmentData);
                }
                break;
            case "university":
                Institution institution = institutionRepository.findByUserId(user.getId()).orElse(null);
                if (institution != null) {
                    Map<String, Object> uniData = new HashMap<>();
                    uniData.put("name", institution.getName());
                    uniData.put("registrationNumber", institution.getRegistrationNumber());
                    uniData.put("city", institution.getCity());
                    uniData.put("address", institution.getAddress());
                    uniData.put("phone", institution.getPhone());
                    uniData.put("website", institution.getWebsite());
                    uniData.put("defaultAuthorityName", institution.getDefaultAuthorityName());
                    uniData.put("defaultAuthorityTitle", institution.getDefaultAuthorityTitle());
                    uniData.put("avatarUrl", institution.getAvatarPath());
                    response.put("university", uniData);
                }
                break;
            case "verifier":
                Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
                if (verifier != null) {
                    Map<String, Object> verifierData = new HashMap<>();
                    verifierData.put("companyName", verifier.getCompanyName());
                    verifierData.put("purpose", verifier.getPurpose());
                    verifierData.put("phone", verifier.getPhone());
                    verifierData.put("website", verifier.getWebsite());
                    verifierData.put("avatarUrl", verifier.getAvatarPath());
                    response.put("verifier", verifierData);
                }
                break;
            case "admin":
                // Admin has no extra nested data based on rules
                break;
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal User user, @Valid @RequestBody ProfileUpdateRequest request) {
        switch (user.getRole()) {
            case "student":
                Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                if (student != null) {
                    if (request.getPhone() != null) student.setPhone(request.getPhone());
                    if (request.getAddress() != null) student.setAddress(request.getAddress());
                    studentRepository.save(student);
                }
                break;
            case "university":
                Institution institution = institutionRepository.findByUserId(user.getId()).orElse(null);
                if (institution != null) {
                    if (request.getPhone() != null) institution.setPhone(request.getPhone());
                    if (request.getAddress() != null) institution.setAddress(request.getAddress());
                    if (request.getWebsite() != null) institution.setWebsite(request.getWebsite());
                    if (request.getDefaultAuthorityName() != null) institution.setDefaultAuthorityName(request.getDefaultAuthorityName());
                    if (request.getDefaultAuthorityTitle() != null) institution.setDefaultAuthorityTitle(request.getDefaultAuthorityTitle());
                    institutionRepository.save(institution);
                }
                break;
            case "verifier":
                Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
                if (verifier != null) {
                    if (request.getPhone() != null) verifier.setPhone(request.getPhone());
                    if (request.getWebsite() != null) verifier.setWebsite(request.getWebsite());
                    if (request.getPurpose() != null) verifier.setPurpose(request.getPurpose());
                    verifierRepository.save(verifier);
                }
                break;
        }
        
        return getProfile(user);
    }



    @PostMapping("/avatar")
    @Transactional
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("avatar") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }

        if (file.getSize() > 300 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image must be under 300KB. Please compress your image and try again."));
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "File must be image (JPEG, PNG, WebP only)"));
        }

        String avatarUrl = fileStorageService.storeFile(file, "avatars", user.getId(), false);
        String oldAvatarUrl = null;

        switch (user.getRole()) {
            case "student":
                Student student = studentRepository.findByUserId(user.getId()).orElseThrow();
                oldAvatarUrl = student.getAvatarPath();
                student.setAvatarPath(avatarUrl);
                studentRepository.save(student);
                break;
            case "university":
                Institution institution = institutionRepository.findByUserId(user.getId()).orElseThrow();
                oldAvatarUrl = institution.getAvatarPath();
                institution.setAvatarPath(avatarUrl);
                institutionRepository.save(institution);
                break;
            case "verifier":
                Verifier verifier = verifierRepository.findByUserId(user.getId()).orElseThrow();
                oldAvatarUrl = verifier.getAvatarPath();
                verifier.setAvatarPath(avatarUrl);
                verifierRepository.save(verifier);
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("message", "Admins do not have avatars"));
        }

        if (oldAvatarUrl != null) {
            fileStorageService.deleteFile(oldAvatarUrl);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "avatarUrl", avatarUrl,
                "message", "Profile picture updated"
        ));
    }

    @DeleteMapping("/avatar")
    @Transactional
    public ResponseEntity<?> removeAvatar(@AuthenticationPrincipal User user) {
        String oldAvatarUrl = null;

        switch (user.getRole()) {
            case "student":
                Student student = studentRepository.findByUserId(user.getId()).orElseThrow();
                oldAvatarUrl = student.getAvatarPath();
                student.setAvatarPath(null);
                studentRepository.save(student);
                break;
            case "university":
                Institution institution = institutionRepository.findByUserId(user.getId()).orElseThrow();
                oldAvatarUrl = institution.getAvatarPath();
                institution.setAvatarPath(null);
                institutionRepository.save(institution);
                break;
            case "verifier":
                Verifier verifier = verifierRepository.findByUserId(user.getId()).orElseThrow();
                oldAvatarUrl = verifier.getAvatarPath();
                verifier.setAvatarPath(null);
                verifierRepository.save(verifier);
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("message", "Admins do not have avatars"));
        }

        if (oldAvatarUrl != null) {
            fileStorageService.deleteFile(oldAvatarUrl);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile picture removed"
        ));
    }



    @PostMapping("/change-request")
    @Transactional
    public ResponseEntity<?> createChangeRequest(
            @AuthenticationPrincipal User user,
            @RequestParam("fieldName") String fieldName,
            @RequestParam("requestedValue") String requestedValue,
            @RequestParam("reason") String reason,
            @RequestParam(value = "supportingDocument", required = false) MultipartFile file) {

        if (reason == null || reason.trim().length() < 20) {
            return ResponseEntity.badRequest().body(Map.of("message", "Reason must be at least 20 characters long"));
        }

        if (requestedValue == null || requestedValue.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Requested value cannot be empty"));
        }

        // Validate allowed fields per role and get current value
        String currentValue = null;
        boolean allowed = false;

        switch (user.getRole()) {
            case "student":
                List<String> studentFields = Arrays.asList("firstName", "middleName", "lastName", "dateOfBirth", "nid");
                if (studentFields.contains(fieldName)) {
                    allowed = true;
                    Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                    if (student != null) {
                        switch (fieldName) {
                            case "firstName": currentValue = student.getFirstName(); break;
                            case "middleName": currentValue = student.getMiddleName(); break;
                            case "lastName": currentValue = student.getLastName(); break;
                            case "dateOfBirth": currentValue = student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : null; break;
                            case "nid": currentValue = student.getNidHash(); break;
                        }
                    }
                }
                break;
            case "university":
                if ("name".equals(fieldName)) {
                    allowed = true;
                    Institution inst = institutionRepository.findByUserId(user.getId()).orElse(null);
                    if (inst != null) currentValue = inst.getName();
                }
                break;
            case "verifier":
                if ("companyName".equals(fieldName)) {
                    allowed = true;
                    Verifier verifier = verifierRepository.findByUserId(user.getId()).orElse(null);
                    if (verifier != null) currentValue = verifier.getCompanyName();
                }
                break;
        }

        if (!allowed) {
            return ResponseEntity.badRequest().body(Map.of("message", "Field not allowed for change request for your role"));
        }

        if (profileChangeRequestRepository.existsByUserIdAndFieldNameAndStatus(user.getId(), fieldName, "pending")) {
            return ResponseEntity.badRequest().body(Map.of("message", "You already have a pending request for this field"));
        }

        String documentUrl = null;
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("message", "Document must be under 5MB"));
            }
            documentUrl = fileStorageService.storeFile(file, "documents", user.getId(), false);
        }

        ProfileChangeRequest request = new ProfileChangeRequest();
        request.setUser(user);
        request.setFieldName(fieldName);
        request.setCurrentValue(currentValue);
        request.setRequestedValue(requestedValue);
        request.setReason(reason);
        request.setSupportingDocumentPath(documentUrl);
        request.setStatus("pending");

        profileChangeRequestRepository.save(request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Change request submitted successfully",
                "data", request
        ));
    }

    @GetMapping("/change-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getChangeRequests(@AuthenticationPrincipal User user) {
        List<ProfileChangeRequest> requests = profileChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Map<String, Object>> result = requests.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", req.getId());
            map.put("fieldName", req.getFieldName());
            map.put("currentValue", req.getCurrentValue());
            map.put("requestedValue", req.getRequestedValue());
            map.put("status", req.getStatus());
            map.put("createdAt", req.getCreatedAt());
            map.put("reviewNotes", req.getReviewNotes());
            map.put("supportingDocumentPath", req.getSupportingDocumentPath());
            return map;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
        ));
    }

    @PatchMapping("/password")
    @Transactional
    public ResponseEntity<?> updatePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordUpdateRequest request,
            HttpServletRequest httpRequest) {

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "The current password is incorrect."
            ));
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "New passwords do not match."
            ));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Blacklist token
        String header = httpRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            blacklistService.blacklist(header.substring(7));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed. Please log in again."
        ));
    }



    @PostMapping("/deactivate")
    @Transactional
    public ResponseEntity<?> deactivateAccount(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String password = body.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required to deactivate your account"));
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Incorrect password"));
        }

        user.setIsDeactivated(true);
        user.setDeactivatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Blacklist current token
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            blacklistService.blacklist(header.substring(7));
        }

        ActivityLog log = new ActivityLog();
        log.setUserId(user.getId());
        log.setAction("ACCOUNT_DEACTIVATED");
        log.setDescription("User deactivated their own account");
        log.setIpAddress(request.getRemoteAddr());
        activityLogRepository.save(log);

        return ResponseEntity.ok(Map.of("success", true, "message", "Account deactivated successfully"));
    }

    @PostMapping("/request-deletion")
    @Transactional
    public ResponseEntity<?> requestDeletion(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String password = body.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Incorrect password"));
        }
        if (accountDeletionRequestRepository.existsByUserIdAndStatus(user.getId(), "pending")) {
            return ResponseEntity.badRequest().body(Map.of("message", "You already have a pending deletion request"));
        }

        String reason = body.getOrDefault("reason", "");

        AccountDeletionRequest req = AccountDeletionRequest.builder()
                .user(user)
                .reason(reason)
                .status("pending")
                .build();
        accountDeletionRequestRepository.save(req);

        // Immediately deactivate account
        user.setIsDeactivated(true);
        user.setDeactivatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Blacklist current token
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            blacklistService.blacklist(header.substring(7));
        }

        ActivityLog log = new ActivityLog();
        log.setUserId(user.getId());
        log.setAction("ACCOUNT_DELETION_REQUESTED");
        log.setDescription("User requested account deletion");
        log.setIpAddress(request.getRemoteAddr());
        activityLogRepository.save(log);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Your account deletion request has been submitted. Your account is now deactivated pending admin review."
        ));
    }

    @DeleteMapping("/request-deletion")
    @Transactional
    public ResponseEntity<?> cancelDeletionRequest(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        AccountDeletionRequest req = accountDeletionRequestRepository
                .findByUserIdAndStatus(user.getId(), "pending")
                .orElse(null);

        if (req == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No pending deletion request found"));
        }

        req.setStatus("cancelled");
        accountDeletionRequestRepository.save(req);

        // Reactivate account
        user.setIsDeactivated(false);
        user.setDeactivatedAt(null);
        userRepository.save(user);

        ActivityLog log = new ActivityLog();
        log.setUserId(user.getId());
        log.setAction("ACCOUNT_DELETION_CANCELLED");
        log.setDescription("User cancelled their deletion request");
        log.setIpAddress(request.getRemoteAddr());
        activityLogRepository.save(log);

        return ResponseEntity.ok(Map.of("success", true, "message", "Deletion request cancelled. Your account has been reactivated."));
    }

    @GetMapping("/activity")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getActivity(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
            ));
        }

        List<ActivityLog> logs = activityLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Map<String, Object>> activities = logs.stream()
                .limit(50)
                .map(log -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", log.getId());
                    item.put("action", log.getAction());
                    item.put("description", log.getDescription());
                    item.put("date", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
                    item.put("created_at", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
                    item.put("ip_address", log.getIpAddress());
                    item.put("metadata", log.getMetadata());
                    return item;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "activities", activities
        ));
    }
}
