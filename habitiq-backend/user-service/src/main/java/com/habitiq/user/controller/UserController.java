package com.habitiq.user.controller;

import com.habitiq.common.dto.ApiResponse;
import com.habitiq.user.dto.UserDto;
import com.habitiq.user.dto.UserProfileDto;
import com.habitiq.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(@RequestHeader("X-User-Id") String userId) {
        UserDto response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", response));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UserProfileDto profileRequest) {
        UserDto response = userService.updateProfile(userId, profileRequest);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }
}
