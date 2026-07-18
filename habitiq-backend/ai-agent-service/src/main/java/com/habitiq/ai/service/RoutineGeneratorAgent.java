package com.habitiq.ai.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineGeneratorAgent {

    private final ChatLanguageModel chatModel;

    private static final String SYSTEM_PROMPT = """
            You are HabitIQ AI, the world's smartest fitness, diet, and habit coaching assistant.
            Your job is to create detailed, personalized weekly diet and workout routines, and track user consistency.
            
            When generating or modifying a routine, you MUST consider all of the following user profile details:
            - Physical: Age, Gender, Height, Weight, Current body fat percentage.
            - Health: Medical history, food allergies, previous injuries.
            - Dietary: Veg/Non-Veg/Vegan, religious constraints, food preferences, preferred cuisines.
            - Lifestyle: Daily schedule, office timing, sleep timing, water intake goals, experience level.
            - Workout: Gym availability vs Home workout preference, preferred workout duration, goal deadlines.
            - Financial: Budget constraints (Low, Medium, High budget diets).
            
            You MUST explain WHY each critical recommendation is made based on their profile.
            
            Guidelines for formatting the routine:
            1. Keep your conversation friendly, motivational, and supportive.
            2. Structure your routine responses in a clear format:
               - Day (e.g., Monday)
               - Time (HH:MM format, 24h)
               - Task Type: MEAL | WORKOUT | SUPPLEMENT | HYDRATION | REST | MEASUREMENT
               - Description: detailed description of what to do/eat/drink, including a brief explanation of why this was chosen.
               - Duration: X minutes (for workouts/meals)
            3. When refining, only adjust what the user asks to change.
            4. After generating a routine, always end with:
               "Are you satisfied with this routine? Type YES to confirm and start tracking, or describe any changes you'd like."
            5. If the user says YES or confirms, respond with:
               "ROUTINE_CONFIRMED: [routine title]"
            6. Keep responses concise but complete.
            """;

    public String chat(String userContext, String userMessage, List<ChatHistoryEntry> history) {
        List<ChatMessage> messages = new ArrayList<>();
        
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.add(UserMessage.from("My profile: " + userContext));
        messages.add(AiMessage.from("Thank you! I have your profile. I'll use this to create your personalized routine. Please tell me your specific goals or any constraints, or just say 'Generate my routine'."));

        for (ChatHistoryEntry entry : history) {
            if ("USER".equals(entry.role())) {
                messages.add(UserMessage.from(entry.content()));
            } else {
                messages.add(AiMessage.from(entry.content()));
            }
        }

        messages.add(UserMessage.from(userMessage));

        Response<AiMessage> response = chatModel.generate(messages);
        String content = response.content().text();
        log.debug("AI response: {}", content);
        return content;
    }

    public String generateFromFileContent(String fileContent, String userContext) {
        String prompt = String.format("""
                The user has uploaded a file with the following fitness/diet content:
                
                ---
                %s
                ---
                
                User Profile: %s
                
                Please analyze this file, extract the routine, enhance it with your expertise, 
                and present it in the standard HabitIQ format. Then ask the user if they 
                want any changes.
                """, fileContent, userContext);

        List<ChatMessage> messages = List.of(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(prompt)
        );

        return chatModel.generate(messages).content().text();
    }

    public record ChatHistoryEntry(String role, String content) {}
}
