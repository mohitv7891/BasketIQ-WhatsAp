package com.grocerybot.repository;

import com.grocerybot.entity.IdempotencyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyLogRepository extends JpaRepository<IdempotencyLog, UUID> {
    Optional<IdempotencyLog> findBySid(String sid);
}
