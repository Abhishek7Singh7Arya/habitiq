package com.habitiq.tracker.repository;

import com.habitiq.tracker.domain.TrackingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrackingSessionRepository extends JpaRepository<TrackingSession, String> {
    List<TrackingSession> findByUserId(String userId);
    Optional<TrackingSession> findByIdAndUserId(String id, String userId);
}
