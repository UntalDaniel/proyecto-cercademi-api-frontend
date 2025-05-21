package com.cercademiurentals.api.dto;

// Lombok @Data removed, explicit getters and setters added
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer"; 
    private UserProfileDto userProfile; 

    // Constructor
    public JwtAuthenticationResponse(String accessToken, UserProfileDto userProfile) {
        this.accessToken = accessToken;
        this.userProfile = userProfile;
    }

    // Getters
    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UserProfileDto getUserProfile() {
        return userProfile;
    }

    // Setters
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setUserProfile(UserProfileDto userProfile) {
        this.userProfile = userProfile;
    }
}