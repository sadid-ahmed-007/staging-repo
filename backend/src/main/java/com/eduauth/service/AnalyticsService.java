package com.eduauth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final DateTimeFormatter YEAR_MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> getAnalytics(int days) {
        int lookbackDays = Math.max(1, days);
        LocalDate startDate = LocalDate.now().minusDays(lookbackDays - 1);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overview", getOverview());
        response.put("trends", getTrends(lookbackDays, startDate));
        response.put("topUniversities", getTopUniversities());
        response.put("recentActivity", getRecentActivity());
        response.put("universityAnalytics", getUniversityAnalytics());
        response.put("verifierAnalytics", getVerifierAnalytics());

        return response;
    }

    private Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        long totalUsers = queryCount("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL");
        long totalStudents = queryCount("SELECT COUNT(*) FROM users WHERE role = 'student' AND deleted_at IS NULL");
        long totalUniversities = queryCount("SELECT COUNT(*) FROM users WHERE role = 'university' AND deleted_at IS NULL");
        long totalVerifiers = queryCount("SELECT COUNT(*) FROM users WHERE role = 'verifier' AND deleted_at IS NULL");
        long totalCertificates = queryCount("SELECT COUNT(*) FROM certificates WHERE deleted_at IS NULL");
        long pendingApprovals = queryCount("SELECT COUNT(*) FROM users WHERE is_approved = 0 AND email_verified_at IS NOT NULL AND deleted_at IS NULL");
        long pendingProfileChanges = queryCount("SELECT COUNT(*) FROM profile_change_requests WHERE status = 'pending'");
        long activityToday = queryCount("SELECT COUNT(*) FROM activity_logs WHERE created_at >= CURDATE()");
        long totalVerifications = queryCount("SELECT COUNT(*) FROM verification_logs");

        overview.put("totalUsers", totalUsers);
        overview.put("totalStudents", totalStudents);
        overview.put("totalUniversities", totalUniversities);
        overview.put("totalVerifiers", totalVerifiers);
        overview.put("totalCertificates", totalCertificates);
        overview.put("pendingApprovals", pendingApprovals);
        overview.put("pendingProfileChanges", pendingProfileChanges);
        overview.put("activityToday", activityToday);
        overview.put("totalVerifications", totalVerifications);

        return overview;
    }

    private Map<String, Object> getTrends(int days, LocalDate startDate) {
        // Query daily registration counts
        Map<String, Long> regMap = queryDailyCounts(
                "SELECT DATE(created_at) as dt, COUNT(*) as cnt FROM users WHERE created_at >= ? GROUP BY DATE(created_at)",
                startDate.atStartOfDay()
        );

        // Query daily certificates issued
        Map<String, Long> certMap = queryDailyCounts(
                "SELECT DATE(issue_date) as dt, COUNT(*) as cnt FROM certificates WHERE issue_date >= ? GROUP BY DATE(issue_date)",
                startDate
        );

        // Query daily verifications
        Map<String, Long> verMap = queryDailyCounts(
                "SELECT DATE(verified_at) as dt, COUNT(*) as cnt FROM verification_logs WHERE verified_at >= ? GROUP BY DATE(verified_at)",
                startDate.atStartOfDay()
        );

        List<Map<String, Object>> registrations = new ArrayList<>();
        List<Map<String, Object>> certificatesIssued = new ArrayList<>();
        List<Map<String, Object>> verifications = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(DATE_FORMATTER);

            registrations.add(Map.of("date", dateStr, "count", regMap.getOrDefault(dateStr, 0L)));
            certificatesIssued.add(Map.of("date", dateStr, "count", certMap.getOrDefault(dateStr, 0L)));
            verifications.add(Map.of("date", dateStr, "count", verMap.getOrDefault(dateStr, 0L)));
        }

        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("registrations", registrations);
        trends.put("certificatesIssued", certificatesIssued);
        trends.put("verifications", verifications);

        return trends;
    }

    private List<Map<String, Object>> getTopUniversities() {
        String sql = """
            SELECT i.name, COUNT(c.id) as certificates_count
            FROM institutions i
            JOIN certificates c ON i.id = c.institution_id
            WHERE c.deleted_at IS NULL
            GROUP BY i.id, i.name
            ORDER BY certificates_count DESC
            LIMIT 5
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", rs.getString("name"));
            row.put("certificates_count", rs.getLong("certificates_count"));
            return row;
        });
    }

    private List<Map<String, Object>> getRecentActivity() {
        String sql = """
            SELECT a.action, a.description, a.created_at,
                   COALESCE(
                       TRIM(CONCAT(COALESCE(s.first_name, ''), ' ', COALESCE(s.last_name, ''))),
                       inst.name,
                       v.company_name,
                       u.email,
                       'System'
                   ) as user_name
            FROM activity_logs a
            LEFT JOIN users u ON a.user_id = u.id
            LEFT JOIN students s ON u.id = s.user_id
            LEFT JOIN institutions inst ON u.id = inst.user_id
            LEFT JOIN verifiers v ON u.id = v.user_id
            ORDER BY a.created_at DESC
            LIMIT 10
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("action", rs.getString("action"));
            String userName = rs.getString("user_name");
            row.put("user", (userName != null && !userName.isBlank()) ? userName : "System");

            Timestamp ts = rs.getTimestamp("created_at");
            row.put("time", ts != null ? ts.toLocalDateTime().format(DATE_TIME_FORMATTER) : null);
            row.put("description", rs.getString("description"));
            return row;
        });
    }

    private Map<String, Object> getUniversityAnalytics() {
        Map<String, Object> map = new LinkedHashMap<>();

        long totalActiveStudents = queryCount("SELECT COUNT(*) FROM enrollments WHERE status = 'active'");
        long certificatesIssuedAllTime = queryCount("SELECT COUNT(*) FROM certificates WHERE deleted_at IS NULL");
        long certificatesIssuedThisMonth = queryCount(
                "SELECT COUNT(*) FROM certificates WHERE MONTH(issue_date) = MONTH(CURDATE()) AND YEAR(issue_date) = YEAR(CURDATE()) AND deleted_at IS NULL"
        );

        map.put("totalActiveStudents", totalActiveStudents);
        map.put("certificatesIssuedAllTime", certificatesIssuedAllTime);
        map.put("certificatesIssuedThisMonth", certificatesIssuedThisMonth);

        // 12-Month enrollment trend
        LocalDate twelveMonthsAgo = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        String trendSql = """
            SELECT DATE_FORMAT(enrollment_date, '%Y-%m') as ym, COUNT(*) as cnt
            FROM enrollments
            WHERE enrollment_date >= ?
            GROUP BY ym
            """;

        Map<String, Long> trendMap = new HashMap<>();
        jdbcTemplate.query(trendSql, rs -> {
            trendMap.put(rs.getString("ym"), rs.getLong("cnt"));
        }, twelveMonthsAgo);

        List<Map<String, Object>> enrollmentTrend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            String key = ym.format(YEAR_MONTH_KEY);
            String label = ym.format(MONTH_FORMATTER);
            enrollmentTrend.add(Map.of("month", label, "count", trendMap.getOrDefault(key, 0L)));
        }
        map.put("enrollmentTrend", enrollmentTrend);

        // Department breakdown (top 10 programs by enrollment)
        String deptSql = """
            SELECT program, COUNT(*) as cnt
            FROM enrollments
            GROUP BY program
            ORDER BY cnt DESC
            LIMIT 10
            """;

        List<Map<String, Object>> departmentBreakdown = jdbcTemplate.query(deptSql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("program", rs.getString("program"));
            row.put("count", rs.getLong("cnt"));
            return row;
        });
        map.put("departmentBreakdown", departmentBreakdown);

        // Per-university summary
        String summarySql = """
            SELECT i.name,
                   (SELECT COUNT(*) FROM enrollments e WHERE e.institution_id = i.id) as enrolled,
                   (SELECT COUNT(*) FROM certificates c WHERE c.institution_id = i.id AND c.deleted_at IS NULL) as issued,
                   (SELECT COUNT(*) FROM enrollments e WHERE e.institution_id = i.id AND e.status = 'graduated') as graduated
            FROM institutions i
            ORDER BY enrolled DESC
            """;

        List<Map<String, Object>> perUniversitySummary = jdbcTemplate.query(summarySql, (rs, rowNum) -> {
            long enrolled = rs.getLong("enrolled");
            long issued = rs.getLong("issued");
            long graduated = rs.getLong("graduated");
            double graduationRate = (enrolled > 0) ? Math.round(((double) graduated / enrolled * 100.0) * 10.0) / 10.0 : 0.0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", rs.getString("name"));
            row.put("enrolled", enrolled);
            row.put("issued", issued);
            row.put("graduation_rate", graduationRate);
            return row;
        });
        map.put("perUniversitySummary", perUniversitySummary);

        return map;
    }

    private Map<String, Object> getVerifierAnalytics() {
        Map<String, Object> map = new LinkedHashMap<>();

        long totalVerifications = queryCount("SELECT COUNT(*) FROM verification_logs");
        long verificationsThisMonth = queryCount(
                "SELECT COUNT(*) FROM verification_logs WHERE MONTH(verified_at) = MONTH(CURDATE()) AND YEAR(verified_at) = YEAR(CURDATE())"
        );
        long activeAccessGrants = queryCount(
                "SELECT COUNT(DISTINCT student_id) FROM verifier_access WHERE expires_at > NOW() AND revoked_at IS NULL"
        );

        long successfulVerifications = queryCount(
                "SELECT COUNT(*) FROM verification_logs WHERE LOWER(verification_result) IN ('success', 'verified')"
        );
        double verificationSuccessRate = (totalVerifications > 0)
                ? Math.round(((double) successfulVerifications / totalVerifications * 100.0) * 10.0) / 10.0
                : 0.0;

        map.put("totalVerifications", totalVerifications);
        map.put("verificationsThisMonth", verificationsThisMonth);
        map.put("activeAccessGrants", activeAccessGrants);
        map.put("verificationSuccessRate", verificationSuccessRate);

        // 12-Month verification trend
        LocalDate twelveMonthsAgo = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        String trendSql = """
            SELECT DATE_FORMAT(verified_at, '%Y-%m') as ym, COUNT(*) as cnt
            FROM verification_logs
            WHERE verified_at >= ?
            GROUP BY ym
            """;

        Map<String, Long> trendMap = new HashMap<>();
        jdbcTemplate.query(trendSql, rs -> {
            trendMap.put(rs.getString("ym"), rs.getLong("cnt"));
        }, twelveMonthsAgo.atStartOfDay());

        List<Map<String, Object>> verificationTrend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            String key = ym.format(YEAR_MONTH_KEY);
            String label = ym.format(MONTH_FORMATTER);
            verificationTrend.add(Map.of("month", label, "count", trendMap.getOrDefault(key, 0L)));
        }
        map.put("verificationTrend", verificationTrend);

        // Top 5 most verified institutions
        String mostVerifiedSql = """
            SELECT i.name, COUNT(v.id) as verifications_count
            FROM institutions i
            JOIN certificates c ON i.id = c.institution_id
            JOIN verification_logs v ON c.id = v.certificate_id
            GROUP BY i.id, i.name
            ORDER BY verifications_count DESC
            LIMIT 5
            """;

        List<Map<String, Object>> mostVerifiedInstitutions = jdbcTemplate.query(mostVerifiedSql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", rs.getString("name"));
            row.put("verifications_count", rs.getLong("verifications_count"));
            return row;
        });
        map.put("mostVerifiedInstitutions", mostVerifiedInstitutions);

        return map;
    }

    private long queryCount(String sql) {
        Long val = jdbcTemplate.queryForObject(sql, Long.class);
        return val != null ? val : 0L;
    }

    private Map<String, Long> queryDailyCounts(String sql, Object param) {
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String date = rs.getString("dt");
            long cnt = rs.getLong("cnt");
            if (date != null) {
                counts.put(date, cnt);
            }
        }, param);
        return counts;
    }
}
