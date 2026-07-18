package com.habitiq.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressSummaryDto {
    private Integer totalGoals;
    private Integer achievedGoals;
    private Integer activeGoals;
    private Double overallCompletionRate;
    private Integer tasksCompletedThisWeek;
    private Integer tasksTotalThisWeek;
    private List<GoalDto> goals;
}
