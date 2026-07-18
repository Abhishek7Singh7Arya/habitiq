package com.habitiq.ai.repository;

import com.habitiq.ai.domain.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, String> {
    Optional<Routine> findByIdAndUserId(String id, String userId);
    List<Routine> findByUserIdAndStatus(String userId, Routine.RoutineStatus status);
}
