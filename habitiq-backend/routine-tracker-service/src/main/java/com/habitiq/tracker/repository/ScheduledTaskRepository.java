package com.habitiq.tracker.repository;

import com.habitiq.tracker.domain.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, String> {
    List<ScheduledTask> findBySessionId(String sessionId);
    List<ScheduledTask> findByStatusAndScheduledAtBefore(ScheduledTask.TaskStatus status, Instant time);
    List<ScheduledTask> findByStatusAndNotifiedAtBefore(ScheduledTask.TaskStatus status, Instant time);
    List<ScheduledTask> findByStatusAndRemindedAtBefore(ScheduledTask.TaskStatus status, Instant time);
}
