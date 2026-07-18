package com.habitiq.notification.service;

import com.habitiq.common.event.TaskDueEvent;
import com.habitiq.common.event.WhatsAppResponseEvent;
import com.habitiq.common.kafka.KafkaTopics;
import com.habitiq.notification.domain.Notification;
import com.habitiq.notification.domain.NotificationLog;
import com.habitiq.notification.repository.NotificationLogRepository;
import com.habitiq.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOrchestrationService {

    private final TwilioService twilioService;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.TASK_DUE, groupId = "notification-service")
    public void handleTaskDue(TaskDueEvent event) {
        log.info("Task due event received for user {} — task: {} — type: {}",
                event.getUserId(), event.getTaskId(), event.getEventType());

        if ("TASK_VOICE_CALL".equals(event.getEventType())) {
            placeVoiceCall(event);
        } else if ("TASK_REMINDER".equals(event.getEventType())) {
            sendWhatsAppNotification(event, 2);
        } else {
            sendWhatsAppNotification(event, 1);
        }
    }

    @Async
    public void sendWhatsAppNotification(TaskDueEvent event, int attempt) {
        String message = buildWhatsAppMessage(event.getDescription(), event.getTaskType(), attempt);

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .scheduledTaskId(event.getTaskId())
                .channel(Notification.Channel.WHATSAPP)
                .status(Notification.Status.PENDING)
                .attempt(attempt)
                .build();
        notification = notificationRepository.save(notification);

        try {
            String sid = twilioService.sendWhatsApp(event.getPhone(), message);
            notification.setStatus(Notification.Status.SENT);
            notification.setExternalId(sid);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);

            log.info("WhatsApp attempt {} sent for task {}", attempt, event.getTaskId());
        } catch (Exception e) {
            notification.setStatus(Notification.Status.FAILED);
            notificationRepository.save(notification);
            log.error("WhatsApp attempt {} failed for task {}: {}", attempt, event.getTaskId(), e.getMessage());
        }
    }

    @Async
    public void placeVoiceCall(TaskDueEvent event) {
        log.info("Escalating to voice call for user {} — task: {}", event.getUserId(), event.getTaskId());
        String message = "time for your " + event.getTaskType().toLowerCase() + ": " + event.getDescription();

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .scheduledTaskId(event.getTaskId())
                .channel(Notification.Channel.VOICE)
                .status(Notification.Status.PENDING)
                .attempt(1)
                .build();
        notification = notificationRepository.save(notification);

        try {
            String sid = twilioService.placeVoiceCall(event.getPhone(), message, event.getTaskId());
            notification.setStatus(Notification.Status.SENT);
            notification.setExternalId(sid);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);
        } catch (Exception e) {
            notification.setStatus(Notification.Status.FAILED);
            notificationRepository.save(notification);
            log.error("Voice call failed for task {}: {}", event.getTaskId(), e.getMessage());
        }
    }

    public void processWhatsAppReply(String fromPhone, String body, String scheduledTaskId) {
        log.info("WhatsApp reply from {}: '{}'", fromPhone, body);

        NotificationLog inboundLog = NotificationLog.builder()
                .channel("WHATSAPP")
                .direction("INBOUND")
                .fromPhone(fromPhone)
                .rawPayload(body)
                .scheduledTaskId(scheduledTaskId)
                .createdAt(Instant.now())
                .build();
        notificationLogRepository.save(inboundLog);

        String normalized = normalizeResponse(body);

        WhatsAppResponseEvent event = WhatsAppResponseEvent.builder()
                .eventType("WHATSAPP_RESPONSE")
                .sourceService("notification-service")
                .matchedTaskId(scheduledTaskId)
                .responseType(normalized)
                .fromPhone(fromPhone)
                .messageBody(body)
                .build();

        try {
            kafkaTemplate.send(KafkaTopics.WHATSAPP_RESPONSE, scheduledTaskId, event);
            log.info("WhatsApp response event published for task ID: {}", scheduledTaskId);
        } catch (Exception e) {
            log.error("Failed to send WHATSAPP_RESPONSE event to Kafka: {}", e.getMessage());
        }
    }

    public void processVoiceGather(String digit, String scheduledTaskId, String calledPhone) {
        String response = switch (digit) {
            case "1" -> "YES";
            case "2" -> "SKIP";
            case "3" -> "NO";
            default -> "NO";
        };
        log.info("Voice gather digit={} mapped to {} for task {}", digit, response, scheduledTaskId);

        WhatsAppResponseEvent event = WhatsAppResponseEvent.builder()
                .eventType("VOICE_RESPONSE")
                .sourceService("notification-service")
                .matchedTaskId(scheduledTaskId)
                .responseType(response)
                .fromPhone(calledPhone)
                .messageBody("Pressed digit: " + digit)
                .build();

        try {
            kafkaTemplate.send(KafkaTopics.WHATSAPP_RESPONSE, scheduledTaskId, event);
            log.info("Voice response event published as WhatsAppResponseEvent on Kafka for task ID: {}", scheduledTaskId);
        } catch (Exception e) {
            log.error("Failed to send VOICE_RESPONSE event to Kafka: {}", e.getMessage());
        }
    }

    private String buildWhatsAppMessage(String taskDescription, String taskType, int attempt) {
        if (attempt == 1) {
            return String.format(
                "🏋️ *HabitIQ Reminder*\n\nHey! It's time for your %s:\n\n*%s*\n\nReply *YES* when done, *NO* if not yet, or *SKIP* to skip this task.",
                taskType.toLowerCase(), taskDescription
            );
        } else {
            return String.format(
                "⏰ *HabitIQ Follow-up*\n\nJust checking — did you complete your %s?\n\n*%s*\n\nReply *YES* if done, *NO* to get a reminder, or *SKIP* to skip.\n\n💪 You've got this! Stay consistent.",
                taskType.toLowerCase(), taskDescription
            );
        }
    }

    private String normalizeResponse(String body) {
        if (body == null) return "NO";
        String upper = body.trim().toUpperCase();
        if (upper.startsWith("YES") || upper.equals("Y") || upper.equals("DONE") || upper.equals("✓")) return "YES";
        if (upper.startsWith("SKIP") || upper.equals("S")) return "SKIP";
        return "NO";
    }
}
