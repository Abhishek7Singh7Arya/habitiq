package com.habitiq.ai.repository;

import com.habitiq.ai.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Optional<Conversation> findByIdAndUserId(String id, String userId);
    List<Conversation> findByUserIdOrderByCreatedAtDesc(String userId);
}
