package com.grocerybot.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Service
public class SessionHealthService {
    private static final Logger log = LoggerFactory.getLogger(SessionHealthService.class);

    private final TwilioOutboundService twilioOutboundService;
    
    @Value("${twilio.owner-number:}")
    private String ownerNumber;

    @Value("${playwright.headless:true}")
    private boolean headless;

    @Value("${playwright.auth-dir:./auth}")
    private String authDir;

    @Value("${playwright.geolocation.lat:12.9716}")
    private double lat;

    @Value("${playwright.geolocation.lon:77.5946}")
    private double lon;

    public SessionHealthService(TwilioOutboundService twilioOutboundService) {
        this.twilioOutboundService = twilioOutboundService;
    }

    public boolean isSessionHealthy(String platform) {
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
                } else {
                    log.warn("Auth file not found for {}. Session will likely be reported as unhealthy.", platformKey);
                }

                try (BrowserContext context = browser.newContext(options);
                     Page page = context.newPage()) {
                    context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
                    
                    String url = platform.equalsIgnoreCase("blinkit") ? "https://blinkit.com/v2/home/" : "https://www.zeptonow.com/cn/";
                    page.navigate(url);
                    page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
                    
                    String currentUrl = page.url();
                    if (currentUrl.contains("login") || currentUrl.contains("auth")) {
                        log.warn("Session for {} appears to be expired. Redirected to: {}", platform, currentUrl);
                        alertOwner(platform);
                        return false;
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error checking session health for {}", platform, e);
            alertOwner(platform);
            return false;
        }
    }
    
    private void alertOwner(String platform) {
        if (ownerNumber != null && !ownerNumber.isBlank() && !ownerNumber.equals("stub_owner")) {
            String message = "🚨 URGENT: The grocery bot session for *" + platform.toUpperCase() + "* has expired! Please run SaveSession to re-authenticate.";
            twilioOutboundService.sendMessage(ownerNumber, message);
        }
    }
}
