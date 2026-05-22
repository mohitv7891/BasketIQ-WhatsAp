package com.grocerybot.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CartComparison {
    private String bestPlatform;
    private BigDecimal bestTotal;
    private boolean isCompleteCart;
    private List<String> missingItems = new ArrayList<>();
    private Map<String, BigDecimal> platformTotals;
    private String savingsMessage;
    private String comparisonMessage;
}
