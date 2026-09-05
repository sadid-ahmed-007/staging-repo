package com.eduauth.dto.dashboard;

import lombok.Data;

@Data
public class UniversityDashboardDto {
    private long totalEnrolled;
    private long graduatedStudents;
    private long certificatesIssued;
    private long pendingWithdrawals;
    private long thisMonthCertificates;
}
