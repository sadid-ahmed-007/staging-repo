package com.eduauth.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO returned for every notification in API responses.
 * Flattens the Laravel data blob into individual readable fields.
 */
@Data
public class NotificationDto {

    /** UUID string (CHAR 36) */
    private String id;

    /** The app-level notification type, e.g. "CERTIFICATE_ISSUED" */
    private String type;

    /** Human-readable title */
    private String title;

    /** Human-readable body message */
    private String message;

    /** Frontend route to navigate to on click (may be null) */
    private String actionUrl;

    /** Extra JSON data as a raw string (may be null or "{}") */
    private String data;

    /** Null means unread */
    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    /** Convenience boolean derived from readAt */
    private boolean read;
}
