package com.habitiq.progress.dto;

import com.habitiq.progress.domain.Goal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalDto {
    private String id;
    private String goalType;
    private Double targetValue;
    private Double currentValue;
    private String unit;
    private LocalDate targetDate;
    private Goal.GoalStatus status;
    private Double progressPercent;
    private Instant createdAt;
}
