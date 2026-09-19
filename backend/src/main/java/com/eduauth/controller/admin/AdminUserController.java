package com.eduauth.controller.admin;

import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.ActivityLog;
import com.eduauth.model.User;
import com.eduauth.repository.ActivityLogRepository;
import com.eduauth.repository.UserRepository;
import com.eduauth.service.EmailService;
import com.eduauth.service.JwtService;
import com.eduauth.service.NotificationService;
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
    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        long pendingCount = userRepository.countByIsApprovedFalseAndEmailVerifiedAtIsNotNull();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminUserService.getUsers(status, role, search, page, size),
                "pending_count", pendingCount
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserDetails(@PathVariable Long id) {
        var userDetails = adminUserService.getUserDetails(id);
        return ResponseEntity.ok(Map.of("success", true, "data", userDetails, "user", userDetails));
    }

    @GetMapping("/{id}/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15", name = "per_page", required = false) int perPage) {

        int pageIndex = Math.max(0, page - 1);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pageIndex, perPage);
        org.springframework.data.domain.Page<ActivityLog> paged = activityLogRepository.findByUserIdOrderByCreatedAtDesc(id, pageable);

        var list = paged.getContent().stream().map(a -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("action", a.getAction() != null ? a.getAction().toLowerCase() : "action");
            m.put("description", a.getDescription());
            m.put("created_at", a.getCreatedAt());
            m.put("ip_address", a.getIpAddress());
            return m;
        }).toList();

        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "data", list,
                "current_page", paged.getNumber() + 1,
                "last_page", Math.max(1, paged.getTotalPages()),
                "total", paged.getTotalElements(),
                "per_page", perPage
        ));
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

        // Notify user: ACCOUNT_APPROVED
        notificationService.createNotification(
                id,
                "ACCOUNT_APPROVED",
                "Account Approved",
                "Your account has been approved. You can now log in and use the platform.",
                "/dashboard",
                Map.of("userId", id)
        );

        return ResponseEntity.ok(Map.of("success", true, "message", "User approved successfully"));
    }
}
