package com.grocerybot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResult {
    private String platform;
    private String rawSearchName;
    private BigDecimal requestedQuantity;
    private String requestedUnit;
    private String scrapedName;
    private BigDecimal price;
    private boolean inStock;
}
