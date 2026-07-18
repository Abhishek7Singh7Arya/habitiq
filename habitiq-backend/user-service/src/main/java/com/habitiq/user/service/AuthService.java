package com.habitiq.user.service;

import com.habitiq.common.event.UserRegisteredEvent;
import com.habitiq.common.exception.HabitIQException;
import com.habitiq.common.kafka.KafkaTopics;
import com.habitiq.user.domain.RefreshToken;
import com.habitiq.user.domain.User;
import com.habitiq.user.domain.UserProfile;
import com.habitiq.user.dto.*;
import com.habitiq.user.repository.RefreshTokenRepository;
import com.habitiq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.refresh-expiry-days:30}")
    private long refreshExpiryDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw HabitIQException.conflict("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw HabitIQException.conflict("Phone number already registered");
        }

        String userId = UUID.randomUUID().toString();

        User user = User.builder()
                .id(userId)
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .createdAt(Instant.now())
                .build();

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .user(user)
                .name(request.getName())
                .age(request.getAge())
                .gender(request.getGender())
                .weightKg(request.getWeightKg())
                .heightCm(request.getHeightCm())
                .fitnessGoal(request.getFitnessGoal())
                .activityLevel(request.getActivityLevel())
                .healthConditions(request.getHealthConditions())
                .dietaryPreferences(request.getDietaryPreferences())
                .budgetPreference(request.getBudgetPreference())
                .gymAccess(request.getGymAccess())
                .build();

        user.setProfile(profile);
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Publish event to Kafka
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .eventType("USER_REGISTERED")
                .sourceService("user-service")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .name(request.getName())
                .build();
        try {
            kafkaTemplate.send(KafkaTopics.USER_REGISTERED, savedUser.getId(), event);
        } catch (Exception e) {
            log.error("Failed to send USER_REGISTERED event to Kafka: {}", e.getMessage());
        }

        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> HabitIQException.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw HabitIQException.unauthorized("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> HabitIQException.unauthorized("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw HabitIQException.unauthorized("Refresh token expired. Please login again.");
        }

        return buildAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public void logout(String tokenValue) {
        refreshTokenRepository.deleteByToken(tokenValue);
    }

    private AuthResponse buildAuthResponse(User user) {
        // Delete old refresh token if any
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plus(refreshExpiryDays, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

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

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .profile(profileDto)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(userDto)
                .build();
    }
}
