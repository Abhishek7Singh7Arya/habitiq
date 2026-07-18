package com.habitiq.tracker.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTask {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TrackingSession session;

    @Column(name = "routine_task_id", nullable = false, length = 36)
    private String routineTaskId;

    @Column(name = "task_description", nullable = false, length = 1000)
    private String taskDescription;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "user_phone", nullable = false)
    private String userPhone;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "reminded_at")
    private Instant remindedAt;

    @Column(name = "call_placed_at")
    private Instant callPlacedAt;

    public enum TaskStatus {
        PENDING,
        NOTIFIED,
        REMINDED,
        CALLED,
        COMPLETED,
        SKIPPED,
        MISSED
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
