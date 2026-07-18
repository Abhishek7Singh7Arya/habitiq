package com.habitiq.progress.service;

import com.habitiq.common.event.TaskStatusUpdatedEvent;
import com.habitiq.common.exception.HabitIQException;
import com.habitiq.common.kafka.KafkaTopics;
import com.habitiq.progress.domain.Goal;
import com.habitiq.progress.domain.ProgressLog;
import com.habitiq.progress.dto.*;
import com.habitiq.progress.repository.GoalRepository;
import com.habitiq.progress.repository.ProgressLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final GoalRepository goalRepository;
    private final ProgressLogRepository progressLogRepository;

    @Transactional
    public GoalDto createGoal(String userId, CreateGoalRequest request) {
        Goal goal = Goal.builder()
                .userId(userId)
                .goalType(request.getGoalType())
                .targetValue(request.getTargetValue())
                .currentValue(request.getCurrentValue())
                .unit(request.getUnit())
                .targetDate(request.getTargetDate())
                .build();
        return toDto(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<GoalDto> getGoals(String userId) {
        return goalRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public void logProgress(String userId, String goalId, LogProgressRequest request) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> HabitIQException.notFound("Goal not found"));

        ProgressLog logEntry = ProgressLog.builder()
                .userId(userId)
                .goal(goal)
                .loggedValue(request.getValue())
                .unit(request.getUnit())
                .notes(request.getNotes())
                .tasksCompletedToday(request.getTasksCompletedToday())
                .tasksTotalToday(request.getTasksTotalToday())
                .build();

        progressLogRepository.save(logEntry);

        goal.setCurrentValue(request.getValue());
        if (request.getValue() >= goal.getTargetValue()) {
            goal.setStatus(Goal.GoalStatus.ACHIEVED);
        }
        goalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public ProgressSummaryDto getSummary(String userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        long achieved = goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACHIEVED).count();
        long active   = goals.stream().filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE).count();

        List<ProgressLog> weekLogs = progressLogRepository.findByUserIdAndLoggedAtAfter(
                userId, java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS));

        int completedThisWeek = weekLogs.stream()
                .mapToInt(l -> l.getTasksCompletedToday() != null ? l.getTasksCompletedToday() : 0).sum();
        int totalThisWeek = weekLogs.stream()
                .mapToInt(l -> l.getTasksTotalToday() != null ? l.getTasksTotalToday() : 0).sum();

        double completionRate = totalThisWeek > 0 ? (completedThisWeek * 100.0 / totalThisWeek) : 0;

        return ProgressSummaryDto.builder()
                .totalGoals(goals.size())
                .achievedGoals((int) achieved)
                .activeGoals((int) active)
                .overallCompletionRate(completionRate)
                .tasksCompletedThisWeek(completedThisWeek)
                .tasksTotalThisWeek(totalThisWeek)
                .goals(goals.stream().map(this::toDto).toList())
                .build();
    }

    private GoalDto toDto(Goal goal) {
        double progress = 0;
        if (goal.getCurrentValue() != null && goal.getTargetValue() != null && goal.getTargetValue() > 0) {
            progress = (goal.getCurrentValue() / goal.getTargetValue()) * 100.0;
        }
        return GoalDto.builder()
                .id(goal.getId())
                .goalType(goal.getGoalType())
                .targetValue(goal.getTargetValue())
                .currentValue(goal.getCurrentValue())
                .unit(goal.getUnit())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus())
                .progressPercent(progress)
                .createdAt(goal.getCreatedAt())
                .build();
    }

    @KafkaListener(topics = KafkaTopics.TASK_STATUS_UPDATED, groupId = "progress-service")
    @Transactional
    public void handleTaskStatusUpdated(TaskStatusUpdatedEvent event) {
        if (event == null || event.getUserId() == null) return;

        log.info("Task status update received in ProgressService — user={}, task={}, status={}",
                event.getUserId(), event.getTaskId(), event.getStatus());

        if (!"COMPLETED".equalsIgnoreCase(event.getStatus()) && !"SKIPPED".equalsIgnoreCase(event.getStatus())) {
            return;
        }

        String userId = event.getUserId();

        goalRepository.findByUserId(userId).stream()
                .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
                .findFirst()
                .ifPresent(goal -> {
                    ProgressLog entry = ProgressLog.builder()
                            .userId(userId)
                            .goal(goal)
                            .loggedValue(0.0)
                            .unit("task")
                            .notes("Auto-logged from task " + event.getTaskId()
                                    + " status update.")
                            .tasksCompletedToday("COMPLETED".equalsIgnoreCase(event.getStatus()) ? 1 : 0)
                            .tasksTotalToday(1)
                            .build();
                    progressLogRepository.save(entry);
                    log.info("Auto-logged progress for user {} from task event", userId);
                });
    }
}
