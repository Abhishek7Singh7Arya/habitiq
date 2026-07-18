package com.habitiq.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class AiConfig {

    @Value("${ai.provider:gemini}")
    private String provider;

    // Gemini Properties
    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    // OpenAI Properties
    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String openaiModel;

    // DeepSeek Properties
    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Initializing AI Provider Layer with active provider: {}", provider);
        
        switch (provider.toLowerCase()) {
            case "openai":
                if (openaiApiKey.isBlank()) {
                    log.warn("OpenAI API key is missing! Set OPENAI_API_KEY environment variable.");
                }
                return OpenAiChatModel.builder()
                        .apiKey(openaiApiKey.isBlank() ? "demo" : openaiApiKey)
                        .modelName(openaiModel)
                        .timeout(Duration.ofSeconds(60))
                        .build();

            case "deepseek":
                if (deepseekApiKey.isBlank()) {
                    log.warn("DeepSeek API key is missing! Set DEEPSEEK_API_KEY environment variable.");
                }
                return OpenAiChatModel.builder()
                        .baseUrl("https://api.deepseek.com/v1")
                        .apiKey(deepseekApiKey.isBlank() ? "demo" : deepseekApiKey)
                        .modelName(deepseekModel)
                        .timeout(Duration.ofSeconds(60))
                        .build();

            case "gemini":
            default:
                if (geminiApiKey.isBlank()) {
                    log.warn("Gemini API key is missing! Set GEMINI_API_KEY environment variable.");
                }
                return GoogleAiGeminiChatModel.builder()
                        .apiKey(geminiApiKey)
                        .modelName(geminiModel)
                        .build();
        }
    }
}
