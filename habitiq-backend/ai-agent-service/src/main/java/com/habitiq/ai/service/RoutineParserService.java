package com.habitiq.ai.service;

import com.habitiq.ai.domain.*;
import com.habitiq.ai.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineParserService {

    private final RoutineRepository routineRepository;

    private static final Pattern DAY_PATTERN =
            Pattern.compile("(?i)^\\s*(monday|tuesday|wednesday|thursday|friday|saturday|sunday|day\\s*\\d+|rest\\s*day)\\s*:?\\s*$",
                    Pattern.MULTILINE);

    private static final Pattern TASK_PATTERN =
            Pattern.compile("(?i)[-*•]?\\s*(\\d{1,2}:\\d{2})\\s*[-–]?\\s*(MEAL|WORKOUT|SUPPLEMENT|HYDRATION|REST|MEASUREMENT)\\s*:?\\s*(.+)",
                    Pattern.MULTILINE);

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("ROUTINE_CONFIRMED:\\s*(.+)");

    @Transactional
    public Routine parseAndSave(String userId, String conversationId,
                                 String lastAiMessage, List<ConversationMessage> messages) {
        String title = extractTitle(lastAiMessage);

        StringBuilder routineText = new StringBuilder();
        for (ConversationMessage msg : messages) {
            if (msg.getRole() == ConversationMessage.MessageRole.ASSISTANT) {
                routineText.append(msg.getContent()).append("\n");
            }
        }

        Routine routine = Routine.builder()
                .userId(userId)
                .conversationId(conversationId)
                .title(title)
                .status(Routine.RoutineStatus.CONFIRMED)
                .build();

        List<RoutineDay> days = parseDays(routineText.toString(), routine);
        routine.setDays(days);

        return routineRepository.save(routine);
    }

    private String extractTitle(String text) {
        Matcher matcher = TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "My HabitIQ Routine";
    }

    private List<RoutineDay> parseDays(String text, Routine routine) {
        List<RoutineDay> days = new ArrayList<>();
        String[] lines = text.split("\n");

        RoutineDay currentDay = null;
        int dayOrder = 0;

        for (String line : lines) {
            Matcher dayMatcher = DAY_PATTERN.matcher(line);
            if (dayMatcher.find()) {
                dayOrder++;
                currentDay = RoutineDay.builder()
                        .routine(routine)
                        .dayOrder(dayOrder)
                        .dayLabel(dayMatcher.group(1).trim())
                        .tasks(new ArrayList<>())
                        .build();
                days.add(currentDay);
                continue;
            }

            if (currentDay != null) {
                Matcher taskMatcher = TASK_PATTERN.matcher(line);
                if (taskMatcher.find()) {
                    LocalTime time = parseTime(taskMatcher.group(1));
                    RoutineTask.TaskType taskType = parseTaskType(taskMatcher.group(2));
                    String description = taskMatcher.group(3).trim();

                    RoutineTask task = RoutineTask.builder()
                            .routineDay(currentDay)
                            .scheduledTime(time)
                            .taskType(taskType)
                            .description(description)
                            .build();
                    currentDay.getTasks().add(task);
                }
            }
        }

        return days;
    }

    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            return LocalTime.of(8, 0);
        }
    }

    private RoutineTask.TaskType parseTaskType(String type) {
        try {
            return RoutineTask.TaskType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RoutineTask.TaskType.WORKOUT;
        }
    }
}
