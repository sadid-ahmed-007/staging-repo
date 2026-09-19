package com.eduauth.controller;

import com.eduauth.dto.notification.NotificationDto;
import com.eduauth.model.User;
import com.eduauth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Notification endpoints — accessible to ALL authenticated users.
 *
 * GET  /api/notifications                     → dropdown (10 unread + 5 read)
 * GET  /api/notifications/all                 → paginated list (filter=all|unread|read)
 * GET  /api/notifications/unread-count        → { count: X }
 * POST /api/notifications/{id}/read           → mark single as read
 * POST /api/notifications/read-all            → mark all as read, returns { marked: X }
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── GET /api/notifications ────────────────────────────────────────────────
    // Navbar bell dropdown: 10 unread + 5 read

    @GetMapping
    public ResponseEntity<?> getDropdownNotifications(@AuthenticationPrincipal User user) {
        List<NotificationDto> notifications = notificationService.getDropdownNotifications(user.getId());
        long unreadCount = notificationService.getUnreadCount(user.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", notifications,
                "unread_count", unreadCount
        ));
    }

    // ── GET /api/notifications/all ────────────────────────────────────────────
    // Full paginated notification list

    @GetMapping("/all")
    public ResponseEntity<?> getAllNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<NotificationDto> result = notificationService.getAllNotifications(
                user.getId(), filter, PageRequest.of(page, size));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result.getContent(),
                "pagination", Map.of(
                        "currentPage",  result.getNumber(),
                        "totalPages",   result.getTotalPages(),
                        "totalItems",   result.getTotalElements(),
                        "perPage",      result.getSize()
                ),
                "unread_count", notificationService.getUnreadCount(user.getId())
        ));
    }

    // ── GET /api/notifications/unread-count ───────────────────────────────────
    // Polled every 30s by the frontend for badge update

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", count
        ));
    }

    // ── POST /api/notifications/{id}/read ────────────────────────────────────
    // Mark a single notification as read

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        NotificationDto dto = notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification marked as read",
                "data", dto
        ));
    }

    // ── POST /api/notifications/read-all ─────────────────────────────────────
    // Mark all unread notifications as read

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal User user) {
        int marked = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", marked + " notification(s) marked as read",
                "marked", marked
        ));
    }
}
