package com.habitiq.progress.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "progress_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressLog {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    @JsonIgnore
    private Goal goal;

    @Column(name = "logged_value", nullable = false)
    private Double loggedValue;

    @Column(nullable = false)
    private String unit;

    private String notes;

    @Column(name = "tasks_completed_today")
    private Integer tasksCompletedToday;

    @Column(name = "tasks_total_today")
    private Integer tasksTotalToday;

    @CreationTimestamp
    @Column(name = "logged_at", updatable = false)
    private Instant loggedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
