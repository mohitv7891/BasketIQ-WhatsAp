package com.grocerybot.tools;

import com.microsoft.playwright.*;
import com.grocerybot.resilience.UserAgentRotator;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class SaveSession {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: mvn exec:java -Dexec.mainClass=com.grocerybot.tools.SaveSession -Dexec.args=\"<blinkit|zepto>\"");
            System.exit(1);
        }

        String platform = args[0].toLowerCase();
        if (!Arrays.asList("blinkit", "zepto").contains(platform)) {
            System.err.println("Unsupported platform: " + platform);
            System.exit(1);
        }

        System.out.println("Starting SaveSession for " + platform + "...");
        
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled"
                    ));

            Browser browser = playwright.chromium().launch(launchOptions);
            
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent(UserAgentRotator.next())
                    .setGeolocation(12.9716, 77.5946)
                    .setPermissions(Arrays.asList("geolocation"));

            BrowserContext context = browser.newContext(contextOptions);

            // Stealth init script
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            Page page = context.newPage();
            
            String url = platform.equals("blinkit") ? "https://blinkit.com/" : "https://www.zeptonow.com/";
            page.navigate(url);

            System.out.println("Please complete the OTP login and set your address (Bengaluru).");
            System.out.println("Once you see the main grocery page and address is correct, press ENTER in this console to save the session.");
            
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();

            // Save state
            String authFile = "auth/" + platform + "-auth.json";
            context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get(authFile)));
            
            System.out.println("Session saved successfully to " + authFile);
            browser.close();
        }
    }
}
