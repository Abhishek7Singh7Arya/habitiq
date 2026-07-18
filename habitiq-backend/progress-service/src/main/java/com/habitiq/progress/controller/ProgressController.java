package com.habitiq.progress.controller;

import com.habitiq.common.dto.ApiResponse;
import com.habitiq.progress.dto.CreateGoalRequest;
import com.habitiq.progress.dto.GoalDto;
import com.habitiq.progress.dto.LogProgressRequest;
import com.habitiq.progress.dto.ProgressSummaryDto;
import com.habitiq.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Goal setting and progress tracking APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/goals")
    @Operation(summary = "Create a new fitness goal")
    public ResponseEntity<ApiResponse<GoalDto>> createGoal(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateGoalRequest request) {
        GoalDto goal = progressService.createGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Goal created successfully", goal));
    }

    @GetMapping("/goals")
    @Operation(summary = "Get all goals for the current user")
    public ResponseEntity<ApiResponse<List<GoalDto>>> getGoals(
            @RequestHeader("X-User-Id") String userId) {
        List<GoalDto> goals = progressService.getGoals(userId);
        return ResponseEntity.ok(ApiResponse.success("Goals fetched successfully", goals));
    }

    @PostMapping("/goals/{goalId}/log")
    @Operation(summary = "Log progress toward a goal")
    public ResponseEntity<ApiResponse<Void>> logProgress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("goalId") String goalId,
            @Valid @RequestBody LogProgressRequest request) {
        progressService.logProgress(userId, goalId, request);
        return ResponseEntity.ok(ApiResponse.success("Progress logged successfully", null));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get a progress summary for the current user")
    public ResponseEntity<ApiResponse<ProgressSummaryDto>> getSummary(
            @RequestHeader("X-User-Id") String userId) {
        ProgressSummaryDto summary = progressService.getSummary(userId);
        return ResponseEntity.ok(ApiResponse.success("Summary fetched successfully", summary));
    }
}
