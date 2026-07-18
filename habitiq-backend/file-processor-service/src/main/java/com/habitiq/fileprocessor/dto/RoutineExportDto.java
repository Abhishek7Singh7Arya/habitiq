package com.habitiq.fileprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineExportDto {
    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private String status;
    private List<RoutineDayDto> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutineDayDto {
        private UUID id;
        private int dayOrder;
        private String dayLabel;
        private String notes;
        private List<RoutineTaskDto> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutineTaskDto {
        private UUID id;
        private String taskType;
        private String scheduledTime;
        private String description;
        private Integer durationMinutes;
        private String notes;
    }
}
