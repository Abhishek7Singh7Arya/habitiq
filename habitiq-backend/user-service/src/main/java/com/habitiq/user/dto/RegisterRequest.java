package com.habitiq.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    private Integer age;
    private String gender;
    private Double weightKg;
    private Double heightCm;
    private String fitnessGoal;
    private String activityLevel;
    private String healthConditions;
    private String dietaryPreferences;
    private String budgetPreference;
    private Boolean gymAccess;
}
