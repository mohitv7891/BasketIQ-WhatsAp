package com.grocerybot.repository;

import com.grocerybot.entity.SearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SearchResultRepository extends JpaRepository<SearchResult, UUID> {
}
