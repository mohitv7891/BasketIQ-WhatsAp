package com.grocerybot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grocerybot.dto.GroceryItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SafeJsonParser {
    private static final Logger log = LoggerFactory.getLogger(SafeJsonParser.class);
    private final ObjectMapper objectMapper;
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[.*?\\]", Pattern.DOTALL);

    public SafeJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<GroceryItemDto> parse(String llmOutput) throws Exception {
        String cleanOutput = llmOutput.replaceAll("```json", "").replaceAll("```", "").trim();
        
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(cleanOutput);
        if (!matcher.find()) {
            throw new Exception("No JSON array found in LLM output: " + llmOutput);
        }
        
        String jsonArrayString = matcher.group(0);
        
        try {
            List<GroceryItemDto> items = objectMapper.readValue(jsonArrayString, new TypeReference<List<GroceryItemDto>>() {});
            
            for (GroceryItemDto item : items) {
                if (item.getName() == null || item.getName().isBlank()) {
                    throw new Exception("Item name cannot be empty");
                }
                if (item.getQuantity() == null || item.getQuantity().doubleValue() <= 0) {
                    throw new Exception("Invalid quantity for item: " + item.getName());
                }
                // Basic unit normalization/validation could go here
            }
            
            return items;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON: {}", jsonArrayString, e);
            throw new Exception("Invalid JSON structure", e);
        }
    }
}
