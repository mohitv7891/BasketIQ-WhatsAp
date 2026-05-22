package com.grocerybot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroceryItemDto {
    private String name;
    private BigDecimal quantity;
    private String unit;
}
