package com.eduauth.service;

import com.eduauth.dto.auth.*;
import com.eduauth.exception.BadRequestException;
import com.eduauth.exception.UnauthorizedException;
import com.eduauth.model.*;
import com.eduauth.repository.*;
import com.eduauth.util.HashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final StudentRepository studentRepository;
    private final InstitutionRepository institutionRepository;
    private final VerifierRepository verifierRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ActivityLogRepository activityLogRepository;
    
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Transactional
    public void register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Delete any existing pending registration for this email
        pendingRegistrationRepository.deleteByEmail(request.getEmail());

        String otp = String.format("%06d", new Random().nextInt(1000000));
        String name = getNameFromRequest(request);

        PendingRegistration pending = new PendingRegistration();
        pending.setEmail(request.getEmail());
        pending.setUserName(name);
        pending.setRegistrationRole(request.getRole());
        pending.setCodeHash(HashUtil.sha256(otp));
        
        try {
            pending.setRegistrationData(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize registration data", e);
        }
        
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pendingRegistrationRepository.save(pending);

        emailService.sendOtpEmail(request.getEmail(), otp, name);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmailAndVerifiedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No pending registration found or already verified"));

        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Code expired. Request a new one.");
        }

        String codeHash = HashUtil.sha256(request.getCode());
        if (!pending.getCodeHash().equals(codeHash)) {
            pending.setAttempts(pending.getAttempts() + 1);
            if (pending.getAttempts() >= 3) {
                pendingRegistrationRepository.delete(pending);
                throw new BadRequestException("Too many failed attempts. Please register again.");
            }
            pendingRegistrationRepository.save(pending);
            throw new BadRequestException("Invalid code");
        }

        // Proceed to create the user
        RegisterRequest regData;
        try {
            regData = objectMapper.readValue(pending.getRegistrationData(), RegisterRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse registration data", e);
        }

        User user = new User();
        user.setEmail(regData.getEmail());
        user.setPassword(passwordEncoder.encode(regData.getPassword()));
        user.setRole(regData.getRole());
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setIsApproved(false);
        user = userRepository.save(user);

        // Create specific profile
        switch (regData.getRole()) {
            case "student":
                Student student = new Student();
                student.setUser(user);
                student.setFirstName(regData.getFirstName());
                student.setMiddleName(regData.getMiddleName());
                student.setLastName(regData.getLastName());
                student.setDateOfBirth(regData.getDateOfBirth());
                student.setGender(regData.getGender());
                if (regData.getNid() != null && !regData.getNid().isEmpty()) {
                    student.setNidHash(HashUtil.sha256(regData.getNid()));
                    // NID encryption would normally happen here, setting it as string for now
                    student.setNidEncrypted(regData.getNid()); 
                }
                student.setPhone(regData.getPhone());
                student.setAddress(regData.getAddress());
                studentRepository.save(student);
                break;
            case "university":
                Institution institution = new Institution();
                institution.setUser(user);
                institution.setName(regData.getInstitutionName());
                institution.setRegistrationNumber(regData.getRegistrationNumber());
                institution.setAddress(regData.getAddress());
                institution.setCity(regData.getCity());
                institution.setPhone(regData.getPhone());
                institution.setWebsite(regData.getWebsite());
                institutionRepository.save(institution);
                break;
            case "verifier":
                Verifier verifier = new Verifier();
                verifier.setUser(user);
                verifier.setCompanyName(regData.getCompanyName());
                verifier.setContactPerson(regData.getContactPerson());
                verifier.setDesignation(regData.getDesignation());
                verifier.setEmail(regData.getEmail());
                verifier.setPhone(regData.getPhone());
                verifier.setPurpose(regData.getPurpose());
                verifier.setAddress(regData.getAddress());
                verifier.setWebsite(regData.getWebsite());
                verifierRepository.save(verifier);
                break;
        }

        // Create User Settings
        UserSettings settings = new UserSettings();
        settings.setUserId(user.getId());
        settings.setPreferences("{\"notifications\":{\"email\":true,\"inApp\":true},\"privacy\":{\"profileVisibility\":\"public\",\"certificateDefault\":\"public\"},\"display\":{\"theme\":\"light\",\"dateFormat\":\"DD/MM/YYYY\"}}");
        userSettingsRepository.save(settings);

        // Activity log
        logActivity(user.getId(), "USER_REGISTERED", "User registered and email verified");

        pending.setVerifiedAt(LocalDateTime.now());
        pendingRegistrationRepository.save(pending);
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmailAndVerifiedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No pending registration found or already verified"));

        String otp = String.format("%06d", new Random().nextInt(1000000));
        pending.setCodeHash(HashUtil.sha256(otp));
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        pending.setAttempts(0);
        pendingRegistrationRepository.save(pending);

        emailService.sendOtpEmail(request.getEmail(), otp, pending.getUserName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        // CHECK 1: Verify the email exists in the system
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // CHECK 2: Verify the password matches the BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // CHECK 3: Verify the user has completed OTP email verification
        if (user.getEmailVerifiedAt() == null) {
            throw new UnauthorizedException("Please verify your email first");
        }

        // CHECK 4: Verify the account has been approved by an administrator
        if (!user.getIsApproved()) {
            throw new UnauthorizedException("Your account is pending admin approval");
        }

        // CHECK 5: Verify the account has not been suspended by an administrator
        if (user.getSuspendedAt() != null) {
            throw new UnauthorizedException("Your account has been suspended");
        }

        String token = jwtService.generateToken(user);
        
        logActivity(user.getId(), "USER_LOGIN", "User logged in from " + ipAddress);

        Object profile = null;
        switch (user.getRole()) {
            case "student":
                profile = studentRepository.findByUserId(user.getId()).orElse(null);
                break;
            case "university":
                profile = institutionRepository.findByUserId(user.getId()).orElse(null);
                break;
            case "verifier":
                profile = verifierRepository.findByUserId(user.getId()).orElse(null);
                break;
        }

        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserPayload.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .isApproved(user.getIsApproved())
                        .emailVerified(user.getEmailVerifiedAt() != null)
                        .profile(profile)
                        .build())
                .build();
    }

    public void logout(String token) {
        tokenBlacklistService.blacklist(token);
        
        try {
            Long userId = jwtService.extractUserId(token);
            logActivity(userId, "USER_LOGOUT", "User logged out");
        } catch (Exception ignored) {
        }
    }
    
    public AuthResponse getMe(String token) {
        Long userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
                
        Object profile = null;
        switch (user.getRole()) {
            case "student":
                profile = studentRepository.findByUserId(user.getId()).orElse(null);
                break;
            case "university":
                profile = institutionRepository.findByUserId(user.getId()).orElse(null);
                break;
            case "verifier":
                profile = verifierRepository.findByUserId(user.getId()).orElse(null);
                break;
        }

        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserPayload.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .isApproved(user.getIsApproved())
                        .emailVerified(user.getEmailVerifiedAt() != null)
                        .profile(profile)
                        .build())
                .build();
    }

    private void logActivity(Long userId, String action, String description) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDescription(description);
        activityLogRepository.save(log);
    }
    
    private String getNameFromRequest(RegisterRequest request) {
        if ("student".equals(request.getRole())) {
            return request.getFirstName() + " " + request.getLastName();
        } else if ("university".equals(request.getRole())) {
            return request.getInstitutionName();
        } else if ("verifier".equals(request.getRole())) {
            return request.getCompanyName();
        }
        return "User";
    }
}
