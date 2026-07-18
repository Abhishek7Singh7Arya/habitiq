package com.habitiq.progress.repository;

import com.habitiq.progress.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, String> {
    List<Goal> findByUserId(String userId);
    Optional<Goal> findByIdAndUserId(String id, String userId);
}
