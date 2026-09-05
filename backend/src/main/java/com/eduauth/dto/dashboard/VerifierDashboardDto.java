package com.eduauth.dto.dashboard;

import lombok.Data;

@Data
public class VerifierDashboardDto {
    private long accessibleStudents;
    private long pendingRequests;
    private long verificationsToday;
    private long totalVerifications;
    private long approvedRequests;
    private long rejectedRequests;
}
