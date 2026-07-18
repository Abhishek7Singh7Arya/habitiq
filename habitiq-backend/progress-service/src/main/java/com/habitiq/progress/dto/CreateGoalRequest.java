package com.habitiq.progress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateGoalRequest {
    @NotBlank
    private String goalType;
    @NotNull
    private Double targetValue;
    private Double currentValue = 0.0;
    @NotBlank
    private String unit;
    @NotNull
    private LocalDate targetDate;
}
