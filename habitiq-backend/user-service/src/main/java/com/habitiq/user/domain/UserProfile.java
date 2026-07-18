package com.habitiq.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class UserProfile {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false)
    private String name;

    private Integer age;

    private String gender;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "fitness_goal")
    private String fitnessGoal;

    @Column(name = "activity_level")
    private String activityLevel;

    @Column(name = "health_conditions")
    private String healthConditions;

    @Column(name = "dietary_preferences")
    private String dietaryPreferences;

    @Column(name = "budget_preference")
    private String budgetPreference;

    @Column(name = "gym_access")
    private Boolean gymAccess;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
