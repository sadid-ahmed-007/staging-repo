package com.eduauth.dto.dashboard;

import lombok.Data;

@Data
public class AdminDashboardDto {
    private long pendingApprovals;
    private long totalUsers;
    private long totalCertificates;
    private long totalUniversities;
    private long totalStudents;
    private long totalVerifiers;
}
