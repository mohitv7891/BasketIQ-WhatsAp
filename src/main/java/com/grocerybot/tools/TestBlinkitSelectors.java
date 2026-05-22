package com.grocerybot.tools;

import com.microsoft.playwright.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class TestBlinkitSelectors {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setStorageStatePath(Paths.get("auth/blinkit-auth.json")));
            Page page = context.newPage();
            
            page.navigate("https://blinkit.com/s/?q=milk", new Page.NavigateOptions().setTimeout(30000));
            // Locate product cards
            String productSelector = "div.tw-relative.tw-flex.tw-h-full.tw-flex-col.tw-items-start";
            page.waitForSelector(productSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
            
            Locator firstProduct = page.locator(productSelector).first();
            String name = firstProduct.locator("div.tw-line-clamp-2").innerText();
            String price = firstProduct.locator("div.tw-text-200.tw-font-semibold").innerText();
            
            System.out.println("FOUND PRODUCT: " + name + " | PRICE: " + price);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
