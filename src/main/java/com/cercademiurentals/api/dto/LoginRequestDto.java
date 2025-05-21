package com.cercademiurentals.api.dto;

// Lombok @Data removed, explicit getters and setters added
public class LoginRequestDto {

    private String identifier; 
    private String password;

    // Constructor por defecto
    public LoginRequestDto() {
    }

    // Getters
    public String getIdentifier() {
        return identifier;
    }

    public String getPassword() {
        return password;
    }

    // Setters
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}