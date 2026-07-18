package com.habitiq.tracker.service;

import com.habitiq.common.event.RoutineConfirmedEvent;
import com.habitiq.common.event.TaskDueEvent;
import com.habitiq.common.event.TaskStatusUpdatedEvent;
import com.habitiq.common.event.WhatsAppResponseEvent;
import com.habitiq.common.kafka.KafkaTopics;
import com.habitiq.tracker.domain.ScheduledTask;
import com.habitiq.tracker.domain.TrackingSession;
import com.habitiq.tracker.repository.ScheduledTaskRepository;
import com.habitiq.tracker.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackerService {

    private final TrackingSessionRepository sessionRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final TaskSchedulerService taskSchedulerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.ROUTINE_CONFIRMED, groupId = "tracker-service")
    @Transactional
    public void handleRoutineConfirmed(RoutineConfirmedEvent event) {
        log.info("Routine confirmed for user {}: routineId={}", event.getUserId(), event.getRoutineId());

        TrackingSession session = TrackingSession.builder()
                .userId(event.getUserId())
                .routineId(event.getRoutineId())
                .startDate(java.time.LocalDate.now())
                .status(TrackingSession.SessionStatus.ACTIVE)
                .build();

        session = sessionRepository.save(session);
        taskSchedulerService.scheduleTasksForSession(session, event.getRoutineId());
        log.info("Tracking session created: {}", session.getId());
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkDueTasks() {
        Instant now = Instant.now();
        List<ScheduledTask> dueTasks = scheduledTaskRepository
                .findByStatusAndScheduledAtBefore(ScheduledTask.TaskStatus.PENDING, now);

        for (ScheduledTask task : dueTasks) {
            task.setStatus(ScheduledTask.TaskStatus.NOTIFIED);
            task.setNotifiedAt(Instant.now());
            scheduledTaskRepository.save(task);

            TaskDueEvent event = TaskDueEvent.builder()
                    .eventType("TASK_DUE")
                    .sourceService("routine-tracker-service")
                    .userId(task.getSession().getUserId())
                    .taskId(task.getId())
                    .taskType(task.getTaskType())
                    .description(task.getTaskDescription())
                    .scheduledTime(task.getScheduledAt().toString())
                    .phone(task.getUserPhone())
                    .build();

            try {
                kafkaTemplate.send(KafkaTopics.TASK_DUE, task.getId(), event);
                log.info("Task due published for task ID: {}", task.getId());
            } catch (Exception e) {
                log.error("Failed to send TASK_DUE event to Kafka: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkReminders() {
        Instant tenMinAgo = Instant.now().minus(10, ChronoUnit.MINUTES);

        List<ScheduledTask> notifiedTasks = scheduledTaskRepository
                .findByStatusAndNotifiedAtBefore(ScheduledTask.TaskStatus.NOTIFIED, tenMinAgo);

        for (ScheduledTask task : notifiedTasks) {
            task.setStatus(ScheduledTask.TaskStatus.REMINDED);
            task.setRemindedAt(Instant.now());
            scheduledTaskRepository.save(task);

            TaskDueEvent event = TaskDueEvent.builder()
                    .eventType("TASK_REMINDER")
                    .sourceService("routine-tracker-service")
                    .userId(task.getSession().getUserId())
                    .taskId(task.getId())
                    .taskType(task.getTaskType())
                    .description(task.getTaskDescription())
                    .scheduledTime(task.getScheduledAt().toString())
                    .phone(task.getUserPhone())
                    .build();

            try {
                kafkaTemplate.send(KafkaTopics.TASK_DUE, task.getId(), event);
                log.info("Reminder published for task ID: {}", task.getId());
            } catch (Exception e) {
                log.error("Failed to send TASK_REMINDER event to Kafka: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkVoiceCallEscalation() {
        Instant thirtySecsAgo = Instant.now().minus(30, ChronoUnit.SECONDS);

        List<ScheduledTask> remindedTasks = scheduledTaskRepository
                .findByStatusAndRemindedAtBefore(ScheduledTask.TaskStatus.REMINDED, thirtySecsAgo);

        for (ScheduledTask task : remindedTasks) {
            task.setStatus(ScheduledTask.TaskStatus.CALLED);
            task.setCallPlacedAt(Instant.now());
            scheduledTaskRepository.save(task);

            TaskDueEvent event = TaskDueEvent.builder()
                    .eventType("TASK_VOICE_CALL")
                    .sourceService("routine-tracker-service")
                    .userId(task.getSession().getUserId())
                    .taskId(task.getId())
                    .taskType(task.getTaskType())
                    .description(task.getTaskDescription())
                    .scheduledTime(task.getScheduledAt().toString())
                    .phone(task.getUserPhone())
                    .build();

            try {
                kafkaTemplate.send(KafkaTopics.TASK_DUE, task.getId(), event);
                log.info("Voice call escalation published for task ID: {}", task.getId());
            } catch (Exception e) {
                log.error("Failed to send TASK_VOICE_CALL event to Kafka: {}", e.getMessage());
            }
        }
    }

    @KafkaListener(topics = KafkaTopics.WHATSAPP_RESPONSE, groupId = "tracker-service")
    @Transactional
    public void handleUserResponse(WhatsAppResponseEvent event) {
        if (event.getMatchedTaskId() == null) return;

        scheduledTaskRepository.findById(event.getMatchedTaskId())
                .ifPresent(task -> {
                    String response = event.getResponseType();
                    ScheduledTask.TaskStatus newStatus;

                    if ("YES".equalsIgnoreCase(response)) {
                        newStatus = ScheduledTask.TaskStatus.COMPLETED;
                        task.setCompletedAt(Instant.now());
                    } else if ("SKIP".equalsIgnoreCase(response) || "NO".equalsIgnoreCase(response)) {
                        newStatus = ScheduledTask.TaskStatus.SKIPPED;
                    } else {
                        return;
                    }

                    task.setStatus(newStatus);
                    scheduledTaskRepository.save(task);

                    TaskStatusUpdatedEvent statusEvent = TaskStatusUpdatedEvent.builder()
                            .eventType("TASK_STATUS_UPDATED")
                            .sourceService("routine-tracker-service")
                            .userId(task.getSession().getUserId())
                            .taskId(task.getId())
                            .status(newStatus.name())
                            .notes("Response: " + event.getMessageBody())
                            .build();

                    try {
                        kafkaTemplate.send(KafkaTopics.TASK_STATUS_UPDATED, task.getId(), statusEvent);
                        log.info("Task {} status updated to {} via WhatsApp", task.getId(), newStatus);
                    } catch (Exception e) {
                        log.error("Failed to send TASK_STATUS_UPDATED event to Kafka: {}", e.getMessage());
                    }
                });
    }
}
