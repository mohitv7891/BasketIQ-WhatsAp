package com.grocerybot.service;

import com.grocerybot.dto.GroceryItemDto;
import com.grocerybot.dto.ProductResult;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

public interface PlatformScraper {
    String getPlatformName();
    ProductResult scrapeItem(BrowserContext context, GroceryItemDto item) throws Exception;
}
