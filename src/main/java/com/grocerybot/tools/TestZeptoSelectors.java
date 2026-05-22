package com.grocerybot.tools;

import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestZeptoSelectors {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setStorageStatePath(Paths.get("auth/zepto-auth.json")));
            Page page = context.newPage();
            
            page.navigate("https://www.zeptonow.com/search?q=milk", new Page.NavigateOptions().setTimeout(30000));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
            page.waitForTimeout(3000); // Wait 3 seconds for UI to settle
            
            String html = page.innerHTML("body");
            Files.write(Paths.get("scratch/zepto.html"), html.getBytes());
            
            String firstProductSelector = "a[href*=\"/pn/\"]";
            page.waitForSelector(firstProductSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
            Locator firstProduct = page.locator(firstProductSelector).first();
            String name = firstProduct.locator("[data-testid=\"product-card-name\"]").innerText();
            String price = firstProduct.locator("[data-testid=\"product-card-price\"]").innerText();
            
            System.out.println("ZEPTO FOUND PRODUCT: " + name + " | PRICE: " + price);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
