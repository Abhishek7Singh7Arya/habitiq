package com.habitiq.ai.controller;

import com.habitiq.ai.dto.ChatRequest;
import com.habitiq.ai.dto.ChatResponse;
import com.habitiq.ai.dto.ConversationSummaryDto;
import com.habitiq.ai.service.ConversationService;
import com.habitiq.common.dto.ApiResponse;
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
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ChatRequest request) {
        ChatResponse response = conversationService.chat(
                userId, request.getConversationId(), request.getMessage(), request.getUserContext());
        return ResponseEntity.ok(ApiResponse.success("Message processed successfully", response));
    }

    @PostMapping("/chat/file")
    public ResponseEntity<ApiResponse<ChatResponse>> chatWithFile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        String fileContent = body.get("extractedText");
        String userContext = body.get("userContext");
        ChatResponse response = conversationService.chatWithFileContent(userId, fileContent, userContext);
        return ResponseEntity.ok(ApiResponse.success("File processed successfully", response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> getConversations(
            @RequestHeader("X-User-Id") String userId) {
        List<ConversationSummaryDto> response = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", response));
    }
}
