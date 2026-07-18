package com.habitiq.notification.controller;

import com.habitiq.common.dto.ApiResponse;
import com.habitiq.notification.domain.Notification;
import com.habitiq.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Query user notification history")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(summary = "Get notification history for the current user")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
            @RequestHeader("X-User-Id") String userId) {
        List<Notification> list = notificationRepository.findByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", list));
    }
}
