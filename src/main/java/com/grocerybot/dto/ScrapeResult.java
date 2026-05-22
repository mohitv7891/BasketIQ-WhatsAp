package com.grocerybot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeResult {
    private String platform;
    private List<ProductResult> products;
    private boolean sessionExpired;
    private boolean isAvailable;

    public static ScrapeResult sessionExpired(String platform) {
        return new ScrapeResult(platform, List.of(), true, false);
    }
}
