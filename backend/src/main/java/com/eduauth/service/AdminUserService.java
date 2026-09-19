package com.eduauth.service;

import com.eduauth.dto.access.AccessGrantListDto;
import com.eduauth.dto.admin.AdminUserDetailDto;
import com.eduauth.dto.admin.AdminUserListDto;
import com.eduauth.dto.admin.SuspendRequestDto;
import com.eduauth.exception.ResourceNotFoundException;
import com.eduauth.model.AccessGrant;
import com.eduauth.model.ActivityLog;
import com.eduauth.model.User;
import com.eduauth.repository.AccessGrantRepository;
import com.eduauth.repository.ActivityLogRepository;
import com.eduauth.repository.UserRepository;
import com.eduauth.repository.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final AccessService accessService;

    public Page<AdminUserListDto> getUsers(String status, String role, String search, int page, int size) {
        Specification<User> spec = UserSpecification.withFilters(status, role, search);
        Page<User> users = userRepository.findAll(spec, PageRequest.of(page, size));

        return users.map(user -> {
            AdminUserListDto dto = new AdminUserListDto();
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setIsApproved(user.getIsApproved());
            dto.setIsEmailVerified(user.getEmailVerifiedAt() != null);
            dto.setIsSuspended(user.getSuspendedAt() != null);
            dto.setCreatedAt(user.getCreatedAt());
            dto.setApprovedAt(user.getApprovedAt());
            
            String profileName = null;
            if ("student".equals(user.getRole()) && user.getStudent() != null) {
                profileName = user.getStudent().getFirstName() + " " + user.getStudent().getLastName();
            } else if ("university".equals(user.getRole()) && user.getInstitution() != null) {
                profileName = user.getInstitution().getName();
            } else if ("verifier".equals(user.getRole()) && user.getVerifier() != null) {
                profileName = user.getVerifier().getCompanyName();
            }
            dto.setProfile(profileName);

            return dto;
        });
    }

    public AdminUserDetailDto getUserDetails(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        AdminUserDetailDto dto = new AdminUserDetailDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setIsApproved(user.getIsApproved());
        dto.setIsEmailVerified(user.getEmailVerifiedAt() != null);
        dto.setIsSuspended(user.getSuspendedAt() != null);
        dto.setCreatedAt(user.getCreatedAt());
        dto.setApprovedAt(user.getApprovedAt());
        dto.setSuspendedAt(user.getSuspendedAt());
        dto.setSuspensionReason(user.getSuspensionReason());

        LocalDateTime now = LocalDateTime.now();

        if ("student".equals(user.getRole())) {
            dto.setProfile(user.getStudent());
            if (user.getStudent() != null) {
                // Who currently has access to their certs
                List<AccessGrant> activeGrants = accessGrantRepository
                        .findByStudentIdAndRevokedAtIsNullAndExpiresAtAfter(user.getStudent().getId(), now);
                List<AccessGrantListDto> activeDtos = activeGrants.stream()
                        .map(g -> accessService.toGrantDto(g, now))
                        .collect(Collectors.toList());
                dto.setAccessGrants(activeDtos);
            }
        } else if ("university".equals(user.getRole())) {
            dto.setProfile(user.getInstitution());
        } else if ("verifier".equals(user.getRole())) {
            dto.setProfile(user.getVerifier());
            if (user.getVerifier() != null) {
                Long verifierId = user.getVerifier().getId();
                long count = accessGrantRepository.countActiveGrantsForVerifier(verifierId, now);
                dto.setActiveAccessCount((int) count);

                // Students this verifier can currently view (active access grants)
                List<AccessGrant> activeGrants = accessGrantRepository
                        .findActiveGrantsForVerifier(verifierId, now);
                List<AccessGrantListDto> activeDtos = activeGrants.stream()
                        .map(g -> accessService.toGrantDto(g, now))
                        .collect(Collectors.toList());
                dto.setAccessGrants(activeDtos);

                // Verifier access history
                List<AccessGrant> allGrants = accessGrantRepository
                        .findAllByVerifierId(verifierId);
                List<AccessGrantListDto> historyDtos = allGrants.stream()
                        .map(g -> accessService.toGrantDto(g, now))
                        .collect(Collectors.toList());
                dto.setAccessHistory(historyDtos);
            }
        }

        return dto;
    }

    public void suspendUser(Long id, SuspendRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        user.setSuspendedAt(LocalDateTime.now());
        user.setSuspensionReason(requestDto.getReason());
        userRepository.save(user);
        
        ActivityLog log = new ActivityLog();
        log.setUserId(id);
        log.setAction("USER_SUSPENDED");
        log.setDescription("User account was suspended. Reason: " + requestDto.getReason());
        activityLogRepository.save(log);
    }

    public void unsuspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        user.setSuspendedAt(null);
        user.setSuspensionReason(null);
        userRepository.save(user);
        
        ActivityLog log = new ActivityLog();
        log.setUserId(id);
        log.setAction("USER_UNSUSPENDED");
        log.setDescription("User account was unsuspended");
        activityLogRepository.save(log);
    }
}
