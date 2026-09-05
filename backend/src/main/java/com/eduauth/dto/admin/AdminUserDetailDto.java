package com.eduauth.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminUserDetailDto {
    private Long id;
    private String email;
    private String role;
    private Boolean isApproved;
    private Boolean isEmailVerified;
    private Boolean isSuspended;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime suspendedAt;
    private String suspensionReason;
    
    // Generic object for role-specific profile data (Student, Institution, Verifier)
    private Object profile;
}
