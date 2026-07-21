package com.habitiq.ai.service;

import com.habitiq.common.event.RoutineConfirmedEvent;
import com.habitiq.common.kafka.KafkaTopics;
import com.habitiq.ai.domain.*;
import com.habitiq.ai.dto.*;
import com.habitiq.ai.repository.*;
import com.habitiq.common.exception.HabitIQException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final RoutineRepository routineRepository;
    private final RoutineGeneratorAgent agent;
    private final RoutineParserService routineParserService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public ChatResponse chat(String userId, String conversationId, String userMessage, String userContext) {
        Conversation conversation;
        if (conversationId != null && !conversationId.isBlank()) {
            conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> HabitIQException.notFound("Conversation not found"));
        } else {
            conversation = Conversation.builder().userId(userId).build();
            conversation = conversationRepository.save(conversation);
        }

        if (conversation.getStatus() == Conversation.ConversationStatus.CONFIRMED) {
            throw HabitIQException.badRequest("Conversation already confirmed. Start a new one.");
        }

        List<RoutineGeneratorAgent.ChatHistoryEntry> history = conversation.getMessages().stream()
                .map(m -> new RoutineGeneratorAgent.ChatHistoryEntry(m.getRole().name(), m.getContent()))
                .toList();

        ConversationMessage userMsg = ConversationMessage.builder()
                .conversation(conversation)
                .role(ConversationMessage.MessageRole.USER)
                .content(userMessage)
                .build();
        conversation.getMessages().add(userMsg);

        String aiResponse;
        try {
            aiResponse = agent.chat(userContext, userMessage, history);
        } catch (Exception e) {
            log.error("AI Provider error for user {}: {}", userId, e.getMessage(), e);
            throw HabitIQException.badRequest("AI Service error: " + e.getMessage() + ". Check GEMINI_API_KEY.");
        }

        ConversationMessage aiMsg = ConversationMessage.builder()
                .conversation(conversation)
                .role(ConversationMessage.MessageRole.ASSISTANT)
                .content(aiResponse)
                .build();
        conversation.getMessages().add(aiMsg);

        boolean confirmed = false;
        String routineId = null;
        if (aiResponse.contains("ROUTINE_CONFIRMED:")) {
            confirmed = true;
            conversation.setStatus(Conversation.ConversationStatus.CONFIRMED);
            
            Routine routine = routineParserService.parseAndSave(userId, conversation.getId(), 
                    aiResponse, conversation.getMessages());
            routineId = routine.getId();

            RoutineConfirmedEvent event = RoutineConfirmedEvent.builder()
                    .eventType("ROUTINE_CONFIRMED")
                    .sourceService("ai-agent-service")
                    .userId(userId)
                    .routineId(routineId)
                    .conversationId(conversation.getId())
                    .build();
            try {
                kafkaTemplate.send(KafkaTopics.ROUTINE_CONFIRMED, userId, event);
            } catch (Exception e) {
                log.error("Failed to send ROUTINE_CONFIRMED event to Kafka: {}", e.getMessage());
            }
            log.info("Routine confirmed for user {}: routineId={}", userId, routineId);
        }

        conversationRepository.save(conversation);

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .message(aiResponse)
                .routineConfirmed(confirmed)
                .routineId(routineId)
                .build();
    }

    @Transactional
    public ChatResponse chatWithFileContent(String userId, String fileContent, String userContext) {
        Conversation conversation = Conversation.builder().userId(userId).build();
        conversation = conversationRepository.save(conversation);

        String aiResponse;
        try {
            aiResponse = agent.generateFromFileContent(fileContent, userContext);
        } catch (Exception e) {
            log.error("AI Provider error analyzing file for user {}: {}", userId, e.getMessage(), e);
            throw HabitIQException.badRequest("AI Service error: " + e.getMessage() + ". Check GEMINI_API_KEY.");
        }

        ConversationMessage aiMsg = ConversationMessage.builder()
                .conversation(conversation)
                .role(ConversationMessage.MessageRole.ASSISTANT)
                .content(aiResponse)
                .build();
        conversation.getMessages().add(aiMsg);
        conversationRepository.save(conversation);

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .message(aiResponse)
                .routineConfirmed(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getUserConversations(String userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(c -> ConversationSummaryDto.builder()
                        .id(c.getId())
                        .status(c.getStatus().name())
                        .messageCount(c.getMessages().size())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public Routine getRoutineByIdAndUserId(String id, String userId) {
        return routineRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> HabitIQException.notFound("Routine not found"));
    }

    @Transactional(readOnly = true)
    public Routine getActiveRoutine(String userId) {
        return routineRepository.findByUserIdAndStatus(userId, Routine.RoutineStatus.CONFIRMED).stream()
                .findFirst()
                .orElseThrow(() -> HabitIQException.notFound("No active confirmed routine found"));
    }
}
