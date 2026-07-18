package com.habitiq.progress.repository;

import com.habitiq.progress.domain.ProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface ProgressLogRepository extends JpaRepository<ProgressLog, String> {
    List<ProgressLog> findByUserIdAndLoggedAtAfter(String userId, Instant time);
}
