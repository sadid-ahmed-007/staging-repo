package com.eduauth.dto.auth;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private UserPayload user;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserPayload {
        private Long id;
        private String email;
        private String role;
        private Boolean isApproved;
        private Boolean emailVerified;   // true if emailVerifiedAt != null
        private Object profile;          // Student / Institution / Verifier map
    }
}
