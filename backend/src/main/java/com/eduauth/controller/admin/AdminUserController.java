package com.eduauth.controller.admin;

import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.ActivityLog;
import com.eduauth.model.User;
import com.eduauth.repository.ActivityLogRepository;
import com.eduauth.repository.UserRepository;
import com.eduauth.service.EmailService;
import com.eduauth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final com.eduauth.service.AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(Map.of("success", true, "data", adminUserService.getUsers(status, role, search, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserDetails(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "data", adminUserService.getUserDetails(id)));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> suspendUser(@PathVariable Long id, @RequestBody com.eduauth.dto.admin.SuspendRequestDto request) {
        adminUserService.suspendUser(id, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "User suspended successfully"));
    }

    @PostMapping("/{id}/unsuspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unsuspendUser(@PathVariable Long id) {
        adminUserService.unsuspendUser(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "User unsuspended successfully"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveUser(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = authHeader.substring(7);
        Long adminId = jwtService.extractUserId(token);

        user.setIsApproved(true);
        user.setApprovedAt(LocalDateTime.now());
        user.setApprovedBy(adminId);
        userRepository.save(user);

        ActivityLog log = new ActivityLog();
        log.setUserId(id);
        log.setAction("USER_APPROVED");
        log.setDescription("User account was approved by admin");
        activityLogRepository.save(log);

        // Best effort to get name for email
        String name = "User";
        if ("student".equals(user.getRole()) && user.getStudent() != null) {
            name = user.getStudent().getFirstName() + " " + user.getStudent().getLastName();
        } else if ("university".equals(user.getRole()) && user.getInstitution() != null) {
            name = user.getInstitution().getName();
        } else if ("verifier".equals(user.getRole()) && user.getVerifier() != null) {
            name = user.getVerifier().getCompanyName();
        }
        
        emailService.sendApprovalEmail(user.getEmail(), name);

        return ResponseEntity.ok(Map.of("success", true, "message", "User approved successfully"));
    }
}
