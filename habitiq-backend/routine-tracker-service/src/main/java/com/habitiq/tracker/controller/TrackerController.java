package com.habitiq.tracker.controller;

import com.habitiq.common.dto.ApiResponse;
import com.habitiq.tracker.domain.ScheduledTask;
import com.habitiq.tracker.domain.TrackingSession;
import com.habitiq.tracker.repository.ScheduledTaskRepository;
import com.habitiq.tracker.repository.TrackingSessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracker")
@RequiredArgsConstructor
@Tag(name = "Routine Tracker", description = "Tracking session and scheduled task APIs")
@SecurityRequirement(name = "bearerAuth")
public class TrackerController {

    private final TrackingSessionRepository sessionRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;

    @GetMapping("/sessions")
    @Operation(summary = "Get all tracking sessions for the current user")
    public ResponseEntity<ApiResponse<List<TrackingSession>>> getSessions(
            @RequestHeader("X-User-Id") String userId) {

        List<TrackingSession> sessions = sessionRepository.findByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/sessions/{sessionId}/tasks")
    @Operation(summary = "Get all scheduled tasks for a tracking session")
    public ResponseEntity<ApiResponse<List<ScheduledTask>>> getTasksForSession(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("sessionId") String sessionId) {

        List<ScheduledTask> tasks = scheduledTaskRepository.findBySessionId(sessionId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/tasks")
    @Operation(summary = "Get tasks for the current user, optionally filtered by status")
    public ResponseEntity<ApiResponse<List<ScheduledTask>>> getTasksByStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(value = "status", required = false) ScheduledTask.TaskStatus status) {

        List<TrackingSession> sessions = sessionRepository.findByUserId(userId);

        List<ScheduledTask> tasks = sessions.stream()
                .flatMap(s -> scheduledTaskRepository.findBySessionId(s.getId()).stream())
                .filter(t -> status == null || t.getStatus() == status)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PutMapping("/sessions/{sessionId}/pause")
    @Operation(summary = "Pause a tracking session")
    public ResponseEntity<ApiResponse<TrackingSession>> pauseSession(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("sessionId") String sessionId) {

        TrackingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(TrackingSession.SessionStatus.PAUSED);
        sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success("Session paused", session));
    }

    @PutMapping("/sessions/{sessionId}/resume")
    @Operation(summary = "Resume a paused tracking session")
    public ResponseEntity<ApiResponse<TrackingSession>> resumeSession(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("sessionId") String sessionId) {

        TrackingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(TrackingSession.SessionStatus.ACTIVE);
        sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.success("Session resumed", session));
    }
}
