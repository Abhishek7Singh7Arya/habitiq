package com.habitiq.ai.controller;

import com.habitiq.ai.dto.ChatRequest;
import com.habitiq.ai.dto.ChatResponse;
import com.habitiq.ai.dto.ConversationSummaryDto;
import com.habitiq.ai.service.ConversationService;
import com.habitiq.common.dto.ApiResponse;
import com.habitiq.common.exception.HabitIQException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody ChatRequest request) {

        if (userId == null || userId.isBlank() || userId.contains(";")) {
            throw HabitIQException.unauthorized("Authentication required: Missing or invalid user identity header");
        }

        ChatResponse response = conversationService.chat(
                userId.trim(), request.getConversationId(), request.getMessage(), request.getUserContext());
        return ResponseEntity.ok(ApiResponse.success("Message processed successfully", response));
    }

    @PostMapping("/chat/file")
    public ResponseEntity<ApiResponse<ChatResponse>> chatWithFile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody Map<String, String> body) {

        if (userId == null || userId.isBlank()) {
            throw HabitIQException.unauthorized("Authentication required: Missing user identity header");
        }

        String fileContent = body.get("extractedText");
        String userContext = body.get("userContext");
        ChatResponse response = conversationService.chatWithFileContent(userId.trim(), fileContent, userContext);
        return ResponseEntity.ok(ApiResponse.success("File processed successfully", response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> getConversations(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            throw HabitIQException.unauthorized("Authentication required: Missing user identity header");
        }

        List<ConversationSummaryDto> response = conversationService.getUserConversations(userId.trim());
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", response));
    }
}
