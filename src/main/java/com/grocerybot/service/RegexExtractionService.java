package com.grocerybot.service;

import com.grocerybot.dto.GroceryItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegexExtractionService {
    private static final Logger log = LoggerFactory.getLogger(RegexExtractionService.class);

    // Matches formats like:
    // 1 L milk
    // 500 g paneer
    // 2 pcs bread
    // Group 1: Quantity (e.g., "1", "500", "2.5")
    // Group 2: Unit (kg, l, pcs, g, ml) - case insensitive
    // Group 3: Item Name (e.g., "milk", "paneer", "bread")
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)^(\\d+(?:\\.\\d+)?)\\s*(kg|l|pcs|g|ml)\\s+(.+)$");

    public List<GroceryItemDto> extractItems(String rawText) throws Exception {
        List<GroceryItemDto> items = new ArrayList<>();
        
        // Split input by newlines or commas
        String[] lines = rawText.split("[\\r\\n,]+");
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Matcher matcher = ITEM_PATTERN.matcher(trimmed);
            if (matcher.find()) {
                GroceryItemDto item = new GroceryItemDto();
                item.setQuantity(new BigDecimal(matcher.group(1)));
                item.setUnit(matcher.group(2).toLowerCase());
                item.setName(matcher.group(3).trim());
                items.add(item);
            } else {
                log.warn("Line did not match expected format and will be ignored: '{}'", trimmed);
            }
        }

        if (items.isEmpty()) {
            throw new Exception("Could not extract any grocery items. Please ensure you use the format: <quantity> <unit> <item name> on separate lines. Valid units are: kg, l, pcs, g, ml.");
        }

        return items;
    }
}
