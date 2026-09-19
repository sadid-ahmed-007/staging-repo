package com.eduauth.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    // Fields that might be submitted across different roles
    private String phone;
    private String address;
    private String website;
    private String purpose;

    @JsonProperty("defaultAuthorityName")
    private String defaultAuthorityName;

    @JsonProperty("defaultAuthorityTitle")
    private String defaultAuthorityTitle;
    
    // Legacy mapping (just in case frontend sends it this way, optional)
    @JsonProperty("default_authority_name")
    public void setDefaultAuthorityNameLegacy(String name) {
        this.defaultAuthorityName = name;
    }

    @JsonProperty("default_authority_title")
    public void setDefaultAuthorityTitleLegacy(String title) {
        this.defaultAuthorityTitle = title;
    }
}
