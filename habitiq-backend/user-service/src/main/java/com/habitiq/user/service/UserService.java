package com.habitiq.user.service;

import com.habitiq.common.exception.HabitIQException;
import com.habitiq.user.domain.User;
import com.habitiq.user.domain.UserProfile;
import com.habitiq.user.dto.UserDto;
import com.habitiq.user.dto.UserProfileDto;
import com.habitiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> HabitIQException.notFound("User not found"));
        return toDto(user);
    }

    @Transactional
    public UserDto updateProfile(String userId, UserProfileDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> HabitIQException.notFound("User not found"));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().userId(userId).user(user).build();
            user.setProfile(profile);
        }

        if (request.getName() != null) profile.setName(request.getName());
        if (request.getAge() != null) profile.setAge(request.getAge());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getWeightKg() != null) profile.setWeightKg(request.getWeightKg());
        if (request.getHeightCm() != null) profile.setHeightCm(request.getHeightCm());
        if (request.getFitnessGoal() != null) profile.setFitnessGoal(request.getFitnessGoal());
        if (request.getActivityLevel() != null) profile.setActivityLevel(request.getActivityLevel());
        if (request.getHealthConditions() != null) profile.setHealthConditions(request.getHealthConditions());
        if (request.getDietaryPreferences() != null) profile.setDietaryPreferences(request.getDietaryPreferences());
        if (request.getBudgetPreference() != null) profile.setBudgetPreference(request.getBudgetPreference());
        if (request.getGymAccess() != null) profile.setGymAccess(request.getGymAccess());

        userRepository.save(user);
        return toDto(user);
    }

    private UserDto toDto(User user) {
        UserProfile profile = user.getProfile();
        UserProfileDto profileDto = profile != null ? UserProfileDto.builder()
                .name(profile.getName())
                .age(profile.getAge())
                .gender(profile.getGender())
                .weightKg(profile.getWeightKg())
                .heightCm(profile.getHeightCm())
                .fitnessGoal(profile.getFitnessGoal())
                .activityLevel(profile.getActivityLevel())
                .healthConditions(profile.getHealthConditions())
                .dietaryPreferences(profile.getDietaryPreferences())
                .budgetPreference(profile.getBudgetPreference())
                .gymAccess(profile.getGymAccess())
                .build() : null;

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .profile(profileDto)
                .build();
    }
}
