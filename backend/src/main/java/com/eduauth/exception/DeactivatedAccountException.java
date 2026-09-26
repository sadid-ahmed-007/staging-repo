package com.eduauth.exception;

import java.time.LocalDateTime;

public class DeactivatedAccountException extends RuntimeException {
    
    private final String email;
    private final LocalDateTime deactivatedAt;

    public DeactivatedAccountException(String message, String email, LocalDateTime deactivatedAt) {
        super(message);
        this.email = email;
        this.deactivatedAt = deactivatedAt;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }
}
