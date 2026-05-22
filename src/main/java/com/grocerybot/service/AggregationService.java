package com.grocerybot.service;

import com.grocerybot.dto.CartComparison;
import com.grocerybot.dto.ProductResult;
import com.grocerybot.dto.ScrapeResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AggregationService {

    public CartComparison aggregate(List<ScrapeResult> scrapeResults) {
        CartComparison comparison = new CartComparison();
        Map<String, BigDecimal> totals = new HashMap<>();
        Map<String, Boolean> isComplete = new HashMap<>();

        for (ScrapeResult sr : scrapeResults) {
            if (sr.isSessionExpired() || !sr.isAvailable()) {
                continue;
            }

            BigDecimal total = BigDecimal.ZERO;
            boolean complete = true;

            for (ProductResult pr : sr.getProducts()) {
                if (pr.isInStock() && pr.getPrice() != null) {
                    total = total.add(pr.getPrice());
                } else {
                    complete = false;
                }
            }

            totals.put(sr.getPlatform(), total);
            isComplete.put(sr.getPlatform(), complete);
        }

        comparison.setPlatformTotals(totals);

        // Find best complete cart
        String bestPlatform = null;
        BigDecimal bestTotal = null;

        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            if (isComplete.getOrDefault(entry.getKey(), false)) {
                if (bestTotal == null || entry.getValue().compareTo(bestTotal) < 0) {
                    bestTotal = entry.getValue();
                    bestPlatform = entry.getKey();
                }
            }
        }

        if (bestPlatform != null) {
            comparison.setBestPlatform(bestPlatform);
            comparison.setBestTotal(bestTotal);
            comparison.setCompleteCart(true);
        } else {
            // Find cheapest incomplete
            for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
                if (bestTotal == null || entry.getValue().compareTo(bestTotal) < 0) {
                    bestTotal = entry.getValue();
                    bestPlatform = entry.getKey();
                }
            }
            comparison.setBestPlatform(bestPlatform);
            comparison.setBestTotal(bestTotal);
            comparison.setCompleteCart(false);
        }

        // Generate comparison message
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *Grocery Bot Comparison*\n");
        for (ScrapeResult sr : scrapeResults) {
            if (sr.isSessionExpired() || !sr.isAvailable()) continue;
            
            sb.append("\n*").append(sr.getPlatform().toUpperCase()).append("*\n");
            for (ProductResult pr : sr.getProducts()) {
                if (pr.isInStock() && pr.getPrice() != null) {
                    sb.append("- ").append(pr.getRequestedQuantity()).append(" ").append(pr.getRequestedUnit())
                      .append(" ").append(pr.getRawSearchName()).append(": ₹").append(pr.getPrice()).append("\n");
                } else {
                    sb.append("- ").append(pr.getRequestedQuantity()).append(" ").append(pr.getRequestedUnit())
                      .append(" ").append(pr.getRawSearchName()).append(": ❌ Out of stock\n");
                }
            }
            sb.append("Total: ₹").append(totals.get(sr.getPlatform()));
            if (!isComplete.getOrDefault(sr.getPlatform(), false)) {
                sb.append(" (Incomplete)");
            }
            sb.append("\n");
        }
        
        if (comparison.getBestPlatform() != null) {
            sb.append("\n🏆 Best Option: *").append(comparison.getBestPlatform().toUpperCase()).append("* at ₹").append(comparison.getBestTotal());
        } else {
            sb.append("\n❌ Could not find items in stock on any platform.");
        }

        comparison.setComparisonMessage(sb.toString());
        return comparison;
    }
}
