package com.habitiq.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private String name;
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
