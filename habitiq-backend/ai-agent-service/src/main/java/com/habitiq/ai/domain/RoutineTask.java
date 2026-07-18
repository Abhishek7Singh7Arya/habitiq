package com.habitiq.ai.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "routine_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutineTask {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_day_id", nullable = false)
    @JsonIgnore
    private RoutineDay routineDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Column(nullable = false)
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    private String notes;

    public enum TaskType {
        MEAL, WORKOUT, SUPPLEMENT, HYDRATION, REST, MEASUREMENT
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
