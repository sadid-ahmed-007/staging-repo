package com.eduauth.controller;

import com.eduauth.dto.profile.PasswordUpdateRequest;
import com.eduauth.dto.profile.ProfileUpdateRequest;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import com.eduauth.service.JwtService;
import com.eduauth.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
