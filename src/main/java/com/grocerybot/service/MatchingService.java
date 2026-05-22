package com.grocerybot.service;

import com.grocerybot.dto.GroceryItemDto;
import com.grocerybot.dto.ProductResult;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchingService {

    private final LevenshteinDistance distance = new LevenshteinDistance();

    public ProductResult findBestMatch(GroceryItemDto requestedItem, List<ProductResult> scrapedResults) {
        ProductResult bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (ProductResult result : scrapedResults) {
            // Simplified match: just compare lowercased names.
            // In reality, this needs to strip units, brands, handle synonyms, etc.
            if (result.getScrapedName() == null) continue;
            
            String target = requestedItem.getName().toLowerCase();
            String scraped = result.getScrapedName().toLowerCase();

            int dist = distance.apply(target, scraped);
            if (dist < minDistance) {
                minDistance = dist;
                bestMatch = result;
            }
        }

        // Threshold check - if distance is too high, we reject it
        // A robust matching algorithm is needed here. For Phase 1, we assume the top result from search is mostly correct.
        return bestMatch;
    }
}
