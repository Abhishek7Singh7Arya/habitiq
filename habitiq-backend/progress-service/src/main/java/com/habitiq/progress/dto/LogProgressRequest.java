package com.habitiq.progress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LogProgressRequest {
    @NotNull
    private Double value;
    @NotBlank
    private String unit;
    private String notes;
    private Integer tasksCompletedToday = 0;
    private Integer tasksTotalToday = 0;
}
