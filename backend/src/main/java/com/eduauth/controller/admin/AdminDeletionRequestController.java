package com.eduauth.controller.admin;

import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.AccountDeletionRequest;
import com.eduauth.model.ActivityLog;
import com.eduauth.model.User;
import com.eduauth.repository.AccountDeletionRequestRepository;
import com.eduauth.repository.ActivityLogRepository;
import com.eduauth.repository.UserRepository;
import com.eduauth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/deletion-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeletionRequestController {

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listRequests(
            @RequestParam(defaultValue = "pending") String status) {

        List<AccountDeletionRequest> requests = deletionRequestRepository.findByStatusOrderByRequestedAtDesc(status);

        List<Map<String, Object>> data = requests.stream().map(req -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", req.getId());
            m.put("user_id", req.getUser().getId());
            m.put("user_email", req.getUser().getEmail());
            m.put("user_role", req.getUser().getRole());
            m.put("reason", req.getReason());
            m.put("status", req.getStatus());
            m.put("requested_at", req.getRequestedAt());
            m.put("completed_at", req.getCompletedAt());
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<?> completeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {

        AccountDeletionRequest req = deletionRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request not found"));

        if (!"pending".equals(req.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request is not pending"));
        }

        User targetUser = req.getUser();

        // Soft-delete: set deleted_at
        targetUser.setDeletedAt(LocalDateTime.now());
        // Ensure account remains deactivated
        targetUser.setIsDeactivated(true);
        userRepository.save(targetUser);

        req.setStatus("completed");
        req.setCompletedAt(LocalDateTime.now());
        req.setCompletedBy(admin);
        deletionRequestRepository.save(req);

        ActivityLog log = new ActivityLog();
        log.setUserId(admin.getId());
        log.setAction("ACCOUNT_DELETED");
        log.setDescription("Account deletion completed for user #" + targetUser.getId() + " (" + targetUser.getEmail() + ")");
        activityLogRepository.save(log);

        return ResponseEntity.ok(Map.of("success", true, "message", "Account deletion completed"));
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {

        AccountDeletionRequest req = deletionRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deletion request not found"));

        if (!"pending".equals(req.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request is not pending"));
        }

        User targetUser = req.getUser();

        // Reactivate account
        targetUser.setIsDeactivated(false);
        targetUser.setDeactivatedAt(null);
        userRepository.save(targetUser);

        req.setStatus("cancelled");
        req.setCompletedAt(LocalDateTime.now());
        req.setCompletedBy(admin);
        deletionRequestRepository.save(req);

        ActivityLog log = new ActivityLog();
        log.setUserId(admin.getId());
        log.setAction("ACCOUNT_DELETION_CANCELLED");
        log.setDescription("Account deletion cancelled for user #" + targetUser.getId() + " by admin");
        activityLogRepository.save(log);

        notificationService.createNotification(
                targetUser.getId(),
                "INFO",
                "Deletion Request Cancelled",
                "Your account deletion request has been cancelled by an administrator. Your account has been reactivated.",
                "/profile",
                Map.of()
        );

        return ResponseEntity.ok(Map.of("success", true, "message", "Deletion request cancelled and account reactivated"));
    }
}
