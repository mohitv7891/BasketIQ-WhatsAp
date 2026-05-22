package com.grocerybot.service;

import com.grocerybot.dto.GroceryItemDto;
import com.grocerybot.dto.ProductResult;
import com.grocerybot.dto.ScrapeResult;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PlaywrightScraperService {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightScraperService.class);

    private final Map<String, PlatformScraper> scrapers;
    
    @Value("${playwright.headless:true}")
    private boolean headless;

    @Value("${playwright.auth-dir:./auth}")
    private String authDir;

    @Value("${playwright.geolocation.lat:12.9716}")
    private double lat;

    @Value("${playwright.geolocation.lon:77.5946}")
    private double lon;

    public PlaywrightScraperService(List<PlatformScraper> scraperList) {
        this.scrapers = scraperList.stream()
                .collect(Collectors.toMap(PlatformScraper::getPlatformName, s -> s));
    }

    public CompletableFuture<ScrapeResult> scrapePlatformAsync(String platform, List<GroceryItemDto> items) {
        return CompletableFuture.supplyAsync(() -> scrapePlatform(platform, items));
    }

    @CircuitBreaker(name = "scraper")
    @Retry(name = "scraper")
    public ScrapeResult scrapePlatform(String platform, List<GroceryItemDto> items) {
        PlatformScraper scraper = scrapers.get(platform.toLowerCase());
        if (scraper == null) {
            log.error("No scraper found for platform: {}", platform);
            return new ScrapeResult(platform, List.of(), false, false);
        }

        List<ProductResult> results = new ArrayList<>();
        boolean anySuccess = false;

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setArgs(Arrays.asList("--disable-blink-features=AutomationControlled"));
            
            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                
                String platformKey = platform.toLowerCase();
                Path authFile = Paths.get(authDir, platformKey + "-auth.json");
                
                Browser.NewContextOptions options = new Browser.NewContextOptions()
                        .setGeolocation(lat, lon)
                        .setPermissions(Arrays.asList("geolocation"));
                        
                if (Files.exists(authFile)) {
                    options.setStorageStatePath(authFile);
                    log.info("Loaded auth state for {} from {}", platformKey, authFile);
                } else {
                    log.warn("Auth file not found for {}: {}. Starting unauthenticated.", platformKey, authFile);
                }

                try (BrowserContext context = browser.newContext(options)) {
                    context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

                    for (GroceryItemDto item : items) {
                        try {
                            ProductResult result = scraper.scrapeItem(context, item);
                            results.add(result);
                            if (result.isInStock()) {
                                anySuccess = true;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to scrape {} on {}", item.getName(), platform, e);
                            ProductResult errorResult = new ProductResult();
                            errorResult.setPlatform(platform);
                            errorResult.setRawSearchName(item.getName());
                            errorResult.setRequestedQuantity(item.getQuantity());
                            errorResult.setRequestedUnit(item.getUnit());
                            errorResult.setInStock(false);
                            results.add(errorResult);
                        }
                    }

                    try {
                        context.storageState(new com.microsoft.playwright.BrowserContext.StorageStateOptions()
                                .setPath(authFile));
                    } catch (Exception e) {
                        log.warn("Failed to write back storage state for {}", platform, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Playwright crashed while scraping {}", platform, e);
            return ScrapeResult.sessionExpired(platform);
        }

        return new ScrapeResult(platform, results, false, anySuccess);
    }
}
