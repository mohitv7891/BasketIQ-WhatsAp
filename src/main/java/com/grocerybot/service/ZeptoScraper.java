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
public class ZeptoScraper implements PlatformScraper {

    @Override
    public String getPlatformName() {
        return "zepto";
    }

    @Override
    public ProductResult scrapeItem(BrowserContext context, GroceryItemDto item) throws Exception {
        String encodedQuery = URLEncoder.encode(item.getName(), StandardCharsets.UTF_8);
        String url = "https://www.zeptonow.com/search?q=" + encodedQuery;

        ProductResult result = new ProductResult();
        result.setPlatform("zepto");
        result.setRawSearchName(item.getName());
        result.setRequestedQuantity(item.getQuantity());
        result.setRequestedUnit(item.getUnit());
        
        try (Page page = context.newPage()) {
            page.navigate(url, new Page.NavigateOptions().setTimeout(30000));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
            
            try {
                String firstProductSelector = "a[href*=\"/pn/\"]";
                page.waitForSelector(firstProductSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
                
                String name = page.locator(firstProductSelector + " [data-testid=\"product-card-name\"]").first().innerText();
                String priceStr = page.locator(firstProductSelector + " [data-testid=\"product-card-price\"]").first().innerText();
                
                priceStr = priceStr.replaceAll("[^0-9.]", "");
                
                result.setScrapedName(name);
                result.setPrice(new BigDecimal(priceStr));
                result.setInStock(true);
            } catch (Exception e) {
                result.setInStock(false);
            }
        }
        
        return result;
    }
}
