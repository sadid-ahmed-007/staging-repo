package com.eduauth.dto.admin;

import com.eduauth.dto.access.AccessGrantListDto;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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

    // Access grants info for student and verifier
    private Integer activeAccessCount;
    private List<AccessGrantListDto> accessGrants;
    private List<AccessGrantListDto> accessHistory;
}
