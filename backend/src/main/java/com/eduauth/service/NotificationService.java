package com.eduauth.service;

import com.eduauth.dto.notification.NotificationDto;
import com.eduauth.model.Notification;
import com.eduauth.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central notification service.
 *
 * Notifications are stored in the `notifications` table using the Laravel morphable format:
 *   - id             CHAR(36) UUID
 *   - notifiable_id  = userId (Long)
 *   - data           TEXT (JSON blob containing type, title, message, action_url, data sub-object)
 *   - read_at        TIMESTAMP (null = unread)
 *
 * All creation flows in the system (AccessService, EnrollmentService, etc.) should call
 * createNotification() from this service.  The method is intentionally non-transactional
 * so that notification failures never roll back the main business operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String LARAVEL_TYPE        = "App\\Notifications\\AppNotification";
    private static final String LARAVEL_NOTIFIABLE  = "App\\Models\\User";

    private final NotificationRepository notificationRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Create and persist a notification record.
     *
     * This method deliberately does NOT propagate exceptions — a notification
     * failure must never break the calling business transaction.
     *
     * @param userId    the user who should receive this notification
     * @param type      app-level type, e.g. "CERTIFICATE_ISSUED"
     * @param title     short human-readable title
     * @param message   longer description
     * @param actionUrl frontend route (e.g. "/student/certificates"), may be null
     * @param data      extra key/value pairs to embed (may be null or empty)
     */
    public void createNotification(Long userId, String type, String title,
                                   String message, String actionUrl,
                                   Map<String, Object> data) {
        if (userId == null) return;
        try {
            String dataJson = buildDataJson(type, title, message, actionUrl, data);

            Notification n = new Notification();
            n.setId(UUID.randomUUID().toString());
            n.setType(LARAVEL_TYPE);
            n.setNotifiableType(LARAVEL_NOTIFIABLE);
            n.setNotifiableId(userId);
            n.setData(dataJson);
            n.setCreatedAt(LocalDateTime.now());
            n.setUpdatedAt(LocalDateTime.now());

            notificationRepository.save(n);
        } catch (Exception ex) {
            // Notification must never break the calling operation
            log.warn("Failed to create notification for user {}: {}", userId, ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — DROPDOWN (10 unread + 5 read)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the latest notifications for the navbar bell dropdown:
     * up to 10 unread (newest first) followed by up to 5 read (newest first).
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getDropdownNotifications(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, PageRequest.of(0, 10))
                .getContent();

        List<Notification> read = notificationRepository
                .findByUserIdAndReadAtIsNotNullOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .getContent();

        List<NotificationDto> result = new ArrayList<>();
        unread.stream().map(this::toDto).forEach(result::add);
        read.stream().map(this::toDto).forEach(result::add);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — ALL (paginated, with filter)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of notifications for the full notification page.
     *
     * @param filter "all" | "unread" | "read"
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getAllNotifications(Long userId, String filter, Pageable pageable) {
        String f = (filter == null || filter.isBlank()) ? "all" : filter.toLowerCase();
        Page<Notification> page;
        switch (f) {
            case "unread":
                page = notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable);
                break;
            case "read":
                page = notificationRepository.findByUserIdAndReadAtIsNotNullOrderByCreatedAtDesc(userId, pageable);
                break;
            default:
                page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toDto);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MARK AS READ — single
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Mark a single notification as read.
     *
     * @throws com.eduauth.exception.ResourceNotFoundException if not found or not owned by user
     */
    @Transactional
    public NotificationDto markAsRead(String notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.eduauth.exception.ResourceNotFoundException(
                        "Notification not found: " + notificationId));

        if (!n.getNotifiableId().equals(userId)) {
            throw new com.eduauth.exception.ResourceNotFoundException(
                    "Notification not found: " + notificationId);
        }

        if (n.getReadAt() == null) {
            LocalDateTime now = LocalDateTime.now();
            n.setReadAt(now);
            n.setUpdatedAt(now);
            n = notificationRepository.save(n);
        }
        return toDto(n);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MARK ALL AS READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Mark all unread notifications for a user as read.
     *
     * @return the number of notifications that were marked read
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNREAD COUNT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build the JSON blob stored in the `data` column.
     * Format: { "type": "...", "title": "...", "message": "...",
     *           "action_url": "...", "data": { key: value, ... } }
     */
    private String buildDataJson(String type, String title, String message,
                                  String actionUrl, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"type\":\"").append(escape(type)).append("\",");
        sb.append("\"title\":\"").append(escape(title)).append("\",");
        sb.append("\"message\":\"").append(escape(message)).append("\",");
        sb.append("\"action_url\":\"").append(escape(actionUrl)).append("\",");
        sb.append("\"data\":{");
        if (data != null && !data.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(entry.getKey())).append("\":");
                Object val = entry.getValue();
                if (val == null) {
                    sb.append("null");
                } else if (val instanceof Number || val instanceof Boolean) {
                    sb.append(val);
                } else {
                    sb.append("\"").append(escape(val.toString())).append("\"");
                }
                first = false;
            }
        }
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Parse the data JSON blob and populate a NotificationDto.
     * Uses simple string parsing — no external JSON library needed since we
     * built the format ourselves.
     */
    NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setReadAt(n.getReadAt());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setRead(n.getReadAt() != null);

        // Parse the data JSON blob
        String raw = n.getData();
        if (raw != null && !raw.isBlank()) {
            dto.setType(extractJsonString(raw, "type"));
            dto.setTitle(extractJsonString(raw, "title"));
            dto.setMessage(extractJsonString(raw, "message"));
            dto.setActionUrl(extractJsonString(raw, "action_url"));

            // Extract nested "data" sub-object as raw JSON string
            int dataStart = raw.indexOf("\"data\":");
            if (dataStart >= 0) {
                int braceStart = raw.indexOf('{', dataStart + 7);
                if (braceStart >= 0) {
                    int depth = 0, i = braceStart;
                    while (i < raw.length()) {
                        char c = raw.charAt(i);
                        if (c == '{') depth++;
                        else if (c == '}') { depth--; if (depth == 0) break; }
                        i++;
                    }
                    dto.setData(raw.substring(braceStart, i + 1));
                }
            }
        }

        // Fallback for type if not parsed from data blob
        if (dto.getType() == null || dto.getType().isBlank()) {
            dto.setType(n.getType());
        }

        return dto;
    }

    /**
     * Minimal JSON string extractor for simple string fields.
     * Handles: "key":"value" patterns.
     */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        // Find closing quote, respecting escaped quotes
        StringBuilder value = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"') { value.append('"'); i += 2; continue; }
                if (next == '\\') { value.append('\\'); i += 2; continue; }
                if (next == 'n') { value.append('\n'); i += 2; continue; }
                if (next == 'r') { value.append('\r'); i += 2; continue; }
                if (next == 't') { value.append('\t'); i += 2; continue; }
            }
            if (c == '"') break;
            value.append(c);
            i++;
        }
        return value.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
