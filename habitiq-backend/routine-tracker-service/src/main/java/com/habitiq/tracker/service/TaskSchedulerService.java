package com.habitiq.tracker.service;

import com.habitiq.common.dto.ApiResponse;
import com.habitiq.tracker.domain.ScheduledTask;
import com.habitiq.tracker.domain.TrackingSession;
import com.habitiq.tracker.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerService {

    private final ScheduledTaskRepository scheduledTaskRepository;
    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public void scheduleTasksForSession(TrackingSession session, String routineId) {
        try {
            log.info("Fetching routine details for routineId: {}", routineId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", session.getUserId());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    "http://ai-agent-service/api/ai/routines/" + routineId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() == null || !response.getBody().isSuccess()) {
                log.warn("Failed to fetch routine details for routineId: {}", routineId);
                return;
            }

            Map<String, Object> routine = response.getBody().getData();
            if (routine == null) {
                log.warn("Routine data is null for routineId: {}", routineId);
                return;
            }

            List<Map<String, Object>> days = (List<Map<String, Object>>) routine.get("days");
            if (days == null) {
                log.warn("No routine days found in routine details for routineId: {}", routineId);
                return;
            }

            LocalDate startDate = session.getStartDate();
            String userPhone = fetchUserPhone(session.getUserId());

            for (int i = 0; i < days.size(); i++) {
                Map<String, Object> day = days.get(i);
                LocalDate taskDate = startDate.plusDays(i);
                List<Map<String, Object>> tasks = (List<Map<String, Object>>) day.get("tasks");

                if (tasks == null) continue;

                for (Map<String, Object> task : tasks) {
                    String timeStr = (String) task.get("scheduledTime");
                    LocalTime time = timeStr != null ? LocalTime.parse(timeStr.substring(0, 5)) : LocalTime.of(8, 0);

                    Instant scheduledAt = taskDate.atTime(time)
                            .atZone(ZoneId.of("Asia/Kolkata"))
                            .toInstant();

                    ScheduledTask scheduledTask = ScheduledTask.builder()
                            .session(session)
                            .routineTaskId((String) task.get("id"))
                            .taskDescription((String) task.get("description"))
                            .taskType((String) task.get("taskType"))
                            .userPhone(userPhone)
                            .scheduledAt(scheduledAt)
                            .status(ScheduledTask.TaskStatus.PENDING)
                            .build();

                    scheduledTaskRepository.save(scheduledTask);
                }
            }

            log.info("Scheduled tasks for session {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to schedule tasks for session {}: {}", session.getId(), e.getMessage(), e);
        }
    }

    private String fetchUserPhone(String userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    "http://user-service/api/users/me",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                Map<String, Object> user = response.getBody().getData();
                if (user != null) {
                    return (String) user.get("phone");
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch user phone for userId {}: {}", userId, e.getMessage());
        }
        return "+919999999999";
    }
}
