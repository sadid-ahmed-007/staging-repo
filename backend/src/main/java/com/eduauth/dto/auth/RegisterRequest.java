package com.eduauth.dto.auth;

import jakarta.validation.constraints.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @JsonProperty("password_confirmation")
    private String confirmPassword;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "student|university|verifier", message = "Role must be student, university, or verifier")
    private String role;

    // ── Student fields ──────────────────────────────────────────────────────
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("middle_name")
    private String middleName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;
    
    private String gender;
    private String nid;
    private String phone;
    private String address;
    private String website;

    // ── University / Institution fields ─────────────────────────────────────
    @JsonProperty("name")
    private String institutionName;
    
    @JsonProperty("registration_number")
    private String registrationNumber;
    
    private String city;

    // ── Verifier fields ─────────────────────────────────────────────────────
    @JsonProperty("company_name")
    private String companyName;
    
    @JsonProperty("contact_person")
    private String contactPerson;
    
    private String designation;
    private String purpose;
}
