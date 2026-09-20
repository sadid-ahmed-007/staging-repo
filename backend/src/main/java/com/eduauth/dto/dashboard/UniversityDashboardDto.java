package com.eduauth.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class UniversityDashboardDto {
    private long totalEnrolled;
    private long graduatedStudents;
    private long certificatesIssued;
    private long pendingWithdrawals;
    private long thisMonthCertificates;
    private List<ProgramBreakdownDto> programBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgramBreakdownDto {
        private String levelName;
        private String shortName;
        private long activeStudents;
        private long certsIssued;
    }
}

