package com.grocerybot.service;

import com.grocerybot.entity.IdempotencyLog;
import com.grocerybot.repository.IdempotencyLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IdempotencyService {
    
    private final IdempotencyLogRepository repository;

    public IdempotencyService(IdempotencyLogRepository repository) {
        this.repository = repository;
    }

    public boolean isDuplicate(String sid) {
        try {
            Optional<IdempotencyLog> existing = repository.findBySid(sid);
            if (existing.isPresent()) {
                return true;
            }
            
            IdempotencyLog log = new IdempotencyLog();
            log.setSid(sid);
            repository.saveAndFlush(log);
            return false;
        } catch (DataIntegrityViolationException e) {
            // Caught a duplicate insert race condition
            return true;
        }
    }
}
