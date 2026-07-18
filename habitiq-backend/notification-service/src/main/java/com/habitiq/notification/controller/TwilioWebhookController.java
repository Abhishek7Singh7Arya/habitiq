package com.habitiq.notification.controller;

import com.habitiq.notification.service.NotificationOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Twilio callback endpoints — do not call directly")
public class TwilioWebhookController {

    private final NotificationOrchestrationService orchestrationService;

    @PostMapping(value = "/whatsapp", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Twilio WhatsApp inbound webhook")
    public ResponseEntity<String> receiveWhatsApp(
            @RequestParam("From") String from,
            @RequestParam("Body") String body,
            @RequestParam(value = "TaskId", required = false) String taskId) {

        log.info("WhatsApp inbound — From: {}, Body: {}, TaskId: {}", from, body, taskId);

        String phone = from.replace("whatsapp:", "");
        orchestrationService.processWhatsAppReply(phone, body, taskId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body("<Response></Response>");
    }

    @PostMapping(value = "/voice/gather", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Twilio Voice gather digit webhook")
    public ResponseEntity<String> voiceGather(
            @RequestParam(value = "Digits", defaultValue = "3") String digits,
            @RequestParam(value = "TaskId", required = false) String taskId,
            @RequestParam(value = "Called", required = false) String calledPhone) {

        log.info("Voice gather — Digits: {}, TaskId: {}, Called: {}", digits, taskId, calledPhone);
        orchestrationService.processVoiceGather(digits, taskId, calledPhone);

        String responseMessage = switch (digits) {
            case "1" -> "Great job! Your task has been marked as completed. Keep up the amazing work!";
            case "2" -> "Task skipped. You will be notified for your next task. Stay on track!";
            default -> "No worries! Remember to complete this task soon. You can do it!";
        };

        String twiml = "<Response><Say voice=\"Polly.Joanna\">" + responseMessage + "</Say></Response>";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(twiml);
    }

    @PostMapping(value = "/whatsapp/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Twilio WhatsApp status callback")
    public ResponseEntity<Void> whatsAppStatus(
            @RequestParam("MessageSid") String sid,
            @RequestParam("MessageStatus") String status) {
        log.info("WhatsApp status update — SID: {}, Status: {}", sid, status);
        return ResponseEntity.ok().build();
    }
}
