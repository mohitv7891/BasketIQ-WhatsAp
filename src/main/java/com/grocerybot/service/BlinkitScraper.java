package com.grocerybot.service;

import com.grocerybot.dto.GroceryItemDto;
import com.grocerybot.dto.ProductResult;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class BlinkitScraper implements PlatformScraper {

    @Override
    public String getPlatformName() {
        return "blinkit";
    }

    @Override
    public ProductResult scrapeItem(BrowserContext context, GroceryItemDto item) throws Exception {
        // Build search URL
        String encodedQuery = URLEncoder.encode(item.getName(), StandardCharsets.UTF_8);
        String url = "https://blinkit.com/s/?q=" + encodedQuery;

        ProductResult result = new ProductResult();
        result.setPlatform("blinkit");
        result.setRawSearchName(item.getName());
        result.setRequestedQuantity(item.getQuantity());
        result.setRequestedUnit(item.getUnit());
        
        try (Page page = context.newPage()) {
            // Navigate and wait for search results
            page.navigate(url, new Page.NavigateOptions().setTimeout(30000));
            
            // Wait for product cards to load (placeholder selector, needs verification)
            // Example selector: ".ProductList__ProductListWrapper-sc-15r32o5-0" or similar
            // Assuming we wait for generic network idle for simplicity initially or a known class
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
            
            // NOTE: The exact selectors must be identified in a real browser session.
            // Placeholder logic below
            try {
                String firstProductSelector = "div.tw-relative.tw-flex.tw-h-full.tw-flex-col.tw-items-start"; 
                page.waitForSelector(firstProductSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
                
                String name = page.locator(firstProductSelector).first().locator("div.tw-line-clamp-2").innerText();
                String priceStr = page.locator(firstProductSelector).first().locator("div.tw-text-200.tw-font-semibold").innerText();
                
                // Cleanup price (e.g. "₹50" -> "50")
                priceStr = priceStr.replaceAll("[^0-9.]", "");
                
                result.setScrapedName(name);
                result.setPrice(new BigDecimal(priceStr));
                result.setInStock(true); // Assuming if it shows up, it's in stock unless "Out of Stock" badge is present
            } catch (Exception e) {
                // Item not found or selectors changed
                result.setInStock(false);
            }
        }
        
        return result;
    }
}
