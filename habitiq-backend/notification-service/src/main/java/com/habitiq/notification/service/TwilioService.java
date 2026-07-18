package com.habitiq.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class TwilioService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-from}")
    private String whatsappFrom;

    @Value("${twilio.voice-from}")
    private String voiceFrom;

    @Value("${twilio.webhook-base-url}")
    private String webhookBaseUrl;

    @PostConstruct
    public void initTwilio() {
        if (!accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully");
        } else {
            log.warn("Twilio credentials missing! Twilio operations will fail.");
        }
    }

    public String sendWhatsApp(String toPhone, String messageBody) {
        try {
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + toPhone),
                    new PhoneNumber(whatsappFrom),
                    messageBody
            ).create();
            log.info("WhatsApp sent to {} — SID: {}", toPhone, message.getSid());
            return message.getSid();
        } catch (Exception e) {
            log.error("Failed to send WhatsApp to {}: {}", toPhone, e.getMessage());
            throw new RuntimeException("WhatsApp send failed: " + e.getMessage(), e);
        }
    }

    public String placeVoiceCall(String toPhone, String taskDescription, String scheduledTaskId) {
        try {
            com.twilio.rest.api.v2010.account.Call call =
                    com.twilio.rest.api.v2010.account.Call.creator(
                            new PhoneNumber(toPhone),
                            new PhoneNumber(voiceFrom),
                            new com.twilio.type.Twiml(buildTwiml(taskDescription, scheduledTaskId))
                    ).create();

            log.info("Voice call placed to {} — SID: {}", toPhone, call.getSid());
            return call.getSid();
        } catch (Exception e) {
            log.error("Failed to place voice call to {}: {}", toPhone, e.getMessage());
            throw new RuntimeException("Voice call failed: " + e.getMessage(), e);
        }
    }

    public String buildTwiml(String taskDescription, String scheduledTaskId) {
        return "<Response>" +
               "<Gather numDigits=\"1\" action=\"" + webhookBaseUrl + "/api/notifications/webhook/voice/gather?TaskId=" + scheduledTaskId + "\" method=\"POST\">" +
               "<Say voice=\"Polly.Joanna\">Hello! This is HabitIQ. " +
               "It is time for your task: " + taskDescription + ". " +
               "Press 1 if you have completed this task. " +
               "Press 2 if you want to skip it. " +
               "Press 3 to mark it as not done.</Say>" +
               "</Gather>" +
               "<Say>We did not receive your input. Goodbye!</Say>" +
               "</Response>";
    }
}
