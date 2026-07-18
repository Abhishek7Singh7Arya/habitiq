package com.habitiq.ai.controller;

import com.habitiq.ai.domain.Routine;
import com.habitiq.ai.service.ConversationService;
import com.habitiq.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final ConversationService conversationService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Routine>> getActiveRoutine(
            @RequestHeader("X-User-Id") String userId) {
        Routine response = conversationService.getActiveRoutine(userId);
        return ResponseEntity.ok(ApiResponse.success("Active routine fetched successfully", response));
    }

    @GetMapping("/{routineId}")
    public ResponseEntity<ApiResponse<Routine>> getRoutine(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("routineId") String routineId) {
        Routine response = conversationService.getRoutineByIdAndUserId(routineId, userId);
        return ResponseEntity.ok(ApiResponse.success("Routine fetched successfully", response));
    }
}
