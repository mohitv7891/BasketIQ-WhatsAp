package com.grocerybot.repository;

import com.grocerybot.entity.Search;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SearchRepository extends JpaRepository<Search, UUID> {
    @EntityGraph(attributePaths = {"user"})
    Optional<Search> findById(UUID id);
}
