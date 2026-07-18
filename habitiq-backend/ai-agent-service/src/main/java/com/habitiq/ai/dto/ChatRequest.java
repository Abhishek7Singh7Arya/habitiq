package com.habitiq.ai.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String conversationId;
    private String message;
    private String userContext;
}
