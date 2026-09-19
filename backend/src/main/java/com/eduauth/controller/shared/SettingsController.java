package com.eduauth.controller.shared;

import com.eduauth.model.User;
import com.eduauth.model.UserSettings;
import com.eduauth.repository.UserSettingsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.eduauth.service.EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<?> getSettings(@AuthenticationPrincipal User user) {
        String role = user != null && user.getRole() != null ? user.getRole() : "student";
        Map<String, Object> settingsData = getDefaultPreferences(role);

        if (user != null) {
            UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElse(null);
            if (userSettings == null) {
                userSettings = new UserSettings();
                userSettings.setUserId(user.getId());
                userSettings.setProfileVisibility("verifiers_only");
                userSettings.setAllowVerifierSearch(true);
                userSettings.setShowEmailToVerifiers(false);
                userSettings.setShowInstitutionToPublic(true);
                try {
                    userSettings.setPreferences(objectMapper.writeValueAsString(settingsData));
                } catch (Exception ignored) {}
                try {
                    userSettings = userSettingsRepository.save(userSettings);
                } catch (Exception ignored) {}
            } else if (userSettings.getPreferences() != null && !userSettings.getPreferences().isBlank()) {
                try {
                    Map<String, Object> saved = objectMapper.readValue(userSettings.getPreferences(), new TypeReference<Map<String, Object>>() {});
                    deepMerge(settingsData, saved);
                } catch (Exception ignored) {}
            }

            if (userSettings != null) {
                settingsData.put("profile_visibility", userSettings.getProfileVisibility() != null ? userSettings.getProfileVisibility() : "verifiers_only");
                settingsData.put("allow_verifier_search", userSettings.getAllowVerifierSearch() != null ? userSettings.getAllowVerifierSearch() : true);
                settingsData.put("show_email_to_verifiers", userSettings.getShowEmailToVerifiers() != null ? userSettings.getShowEmailToVerifiers() : false);
                settingsData.put("show_institution_to_public", userSettings.getShowInstitutionToPublic() != null ? userSettings.getShowInstitutionToPublic() : true);
            }
        }

        Map<String, Object> account = new HashMap<>();
        if (user != null) {
            account.put("email", user.getEmail());
            account.put("role", user.getRole());
            account.put("is_approved", Boolean.TRUE.equals(user.getIsApproved()));
            account.put("email_verified_at", user.getEmailVerifiedAt() != null ? user.getEmailVerifiedAt().toString() : null);
            account.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            account.put("updated_at", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("settings", settingsData);
        response.put("role", user != null ? user.getRole() : "");
        response.put("account", account);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<?> updateSettings(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> inputSettings = payload.get("settings") instanceof Map 
                ? (Map<String, Object>) payload.get("settings") 
                : new HashMap<>();

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setUserId(user.getId());
            return s;
        });

        if (inputSettings.containsKey("profile_visibility")) {
            userSettings.setProfileVisibility((String) inputSettings.remove("profile_visibility"));
        }
        if (inputSettings.containsKey("allow_verifier_search")) {
            Object v = inputSettings.remove("allow_verifier_search");
            userSettings.setAllowVerifierSearch(v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v)));
        }
        if (inputSettings.containsKey("show_email_to_verifiers")) {
            Object v = inputSettings.remove("show_email_to_verifiers");
            userSettings.setShowEmailToVerifiers(v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v)));
        }
        if (inputSettings.containsKey("show_institution_to_public")) {
            Object v = inputSettings.remove("show_institution_to_public");
            userSettings.setShowInstitutionToPublic(v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v)));
        }

        Map<String, Object> currentPrefs = getDefaultPreferences(user.getRole());
        if (userSettings.getPreferences() != null && !userSettings.getPreferences().isBlank()) {
            try {
                Map<String, Object> saved = objectMapper.readValue(userSettings.getPreferences(), new TypeReference<Map<String, Object>>() {});
                deepMerge(currentPrefs, saved);
            } catch (Exception ignored) {}
        }
        deepMerge(currentPrefs, inputSettings);

        try {
            userSettings.setPreferences(objectMapper.writeValueAsString(currentPrefs));
        } catch (Exception ignored) {}

        userSettingsRepository.save(userSettings);

        currentPrefs.put("profile_visibility", userSettings.getProfileVisibility());
        currentPrefs.put("allow_verifier_search", userSettings.getAllowVerifierSearch());
        currentPrefs.put("show_email_to_verifiers", userSettings.getShowEmailToVerifiers());
        currentPrefs.put("show_institution_to_public", userSettings.getShowInstitutionToPublic());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Settings updated successfully.",
                "settings", currentPrefs
        ));
    }

    @PostMapping("/reset")
    @Transactional
    public ResponseEntity<?> resetSettings(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        UserSettings userSettings = userSettingsRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setUserId(user.getId());
            return s;
        });

        Map<String, Object> defs = getDefaultPreferences(user.getRole());
        try {
            userSettings.setPreferences(objectMapper.writeValueAsString(defs));
        } catch (Exception ignored) {}
        userSettings.setProfileVisibility("verifiers_only");
        userSettings.setAllowVerifierSearch(true);
        userSettings.setShowEmailToVerifiers(false);
        userSettings.setShowInstitutionToPublic(true);
        userSettingsRepository.save(userSettings);

        defs.put("profile_visibility", "verifiers_only");
        defs.put("allow_verifier_search", true);
        defs.put("show_email_to_verifiers", false);
        defs.put("show_institution_to_public", true);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Settings reset to default.",
                "settings", defs
        ));
    }

    @GetMapping("/debug-enrollment")
    public ResponseEntity<?> debugEnrollment() {
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 25);
            enrollmentService.getEnrollments(1L, "all", null, pageable);
            return ResponseEntity.ok(Map.of("success", true, "message", "No error"));
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage(), "stacktrace", sw.toString()));
        }
    }

    private Map<String, Object> getDefaultPreferences(String role) {
        Map<String, Object> emailNotifs = new HashMap<>();
        emailNotifs.put("certificate_issued", true);
        emailNotifs.put("enrollment_changes", true);
        emailNotifs.put("access_requests", true);
        emailNotifs.put("profile_changes", true);
        emailNotifs.put("security_alerts", true);

        Map<String, Object> inAppNotifs = new HashMap<>();
        inAppNotifs.put("certificate_issued", true);
        inAppNotifs.put("enrollment_changes", true);
        inAppNotifs.put("access_requests", true);
        inAppNotifs.put("profile_changes", true);
        inAppNotifs.put("security_alerts", true);

        Map<String, Object> notifications = new HashMap<>();
        notifications.put("email", emailNotifs);
        notifications.put("in_app", inAppNotifs);
        notifications.put("frequency", "instant");

        Map<String, Object> privacy = new HashMap<>();
        privacy.put("certificate_default", "public");
        privacy.put("profile_visibility", "public");
        privacy.put("show_email_to_verifiers", false);
        privacy.put("show_phone_to_verifiers", false);
        privacy.put("allow_university_search", true);

        Map<String, Object> display = new HashMap<>();
        display.put("theme", "system");
        display.put("date_format", "DD/MM/YYYY");
        display.put("timezone", "Asia/Dhaka");
        display.put("items_per_page", 25);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("notifications", notifications);
        prefs.put("privacy", privacy);
        prefs.put("display", display);

        if ("student".equalsIgnoreCase(role)) {
            Map<String, Object> certPrefs = new HashMap<>();
            certPrefs.put("default_visibility", "private");
            certPrefs.put("auto_approve_verifier", false);
            certPrefs.put("default_access_duration", 30);
            certPrefs.put("notify_certificate_viewed", true);
            prefs.put("certificate_preferences", certPrefs);

            Map<String, Object> enrollPrefs = new HashMap<>();
            enrollPrefs.put("notify_status_changes", true);
            enrollPrefs.put("notify_graduation_extended", true);
            prefs.put("enrollment_preferences", enrollPrefs);
        } else if ("university".equalsIgnoreCase(role)) {
            Map<String, Object> instPrefs = new HashMap<>();
            instPrefs.put("auto_graduate_on_certificate", true);
            instPrefs.put("auto_generate_student_ids", true);
            instPrefs.put("student_id_prefix", "");
            instPrefs.put("default_certificate_prefix", "BSC");
            instPrefs.put("student_id_format", null);
            prefs.put("institution_preferences", instPrefs);

            Map<String, Object> enrollSettings = new HashMap<>();
            enrollSettings.put("default_session_duration_years", 4);
            prefs.put("enrollment_settings", enrollSettings);
        } else if ("verifier".equalsIgnoreCase(role)) {
            Map<String, Object> verifPrefs = new HashMap<>();
            verifPrefs.put("auto_log_verifications", true);
            prefs.put("verification_preferences", verifPrefs);
        }

        return prefs;
    }

    @SuppressWarnings("unchecked")
    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        if (source == null) return;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof Map && target.get(key) instanceof Map) {
                deepMerge((Map<String, Object>) target.get(key), (Map<String, Object>) val);
            } else {
                target.put(key, val);
            }
        }
    }
}
