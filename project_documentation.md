# Grocery Bot (BasketIQ): End-to-End Pipeline Documentation

The Grocery Bot is a WhatsApp-based microservice that allows users to send a raw grocery list via WhatsApp. The system parses the text, asynchronously scrapes pricing from multiple quick-commerce platforms (Blinkit, Zepto), aggregates the cart totals, and replies with an itemized price comparison.

---

## 🏗 System Architecture & Pipeline Phases

The pipeline is entirely event-driven, beginning with a webhook payload from Twilio.

### Phase 1: Webhook Reception & Pre-Processing
1. **Twilio Webhook:** A user sends a WhatsApp message. Twilio forwards the payload to our Spring Boot backend (`WebhookController`).
2. **Initial Validation:** The controller extracts the user's phone number (`From`) and the raw grocery text (`Body`).
3. **Database Setup:** 
   - Looks up the user in PostgreSQL. If new, it creates a `User` entity.
   - Generates a UUID for the search session and creates a `Search` entity to track the request lifecycle.
4. **Asynchronous Handoff:** To comply with Twilio's webhook timeout (15 seconds) and avoid blocking the HTTP thread during slow web scraping, the controller immediately hands off the request to the `PipelineCoordinator` via an `@Async` method and returns a generic "Got it! Checking prices..." response back to Twilio.

### Phase 2: NLP Extraction
1. **Unstructured to Structured Data:** The raw WhatsApp text (e.g., "1 L milk, 500g paneer") is processed into a list of Java objects (`GroceryItemDto`).
2. **Dual Mechanism Strategy:**
   - **OpenAI API:** Attempts to use an LLM to accurately normalize the item, quantity, and unit.
   - **Regex Fallback:** If the API fails (e.g., due to budget constraints or network timeouts), the `RegexExtractionService` takes over, using regular expressions to parse standard line-by-line inputs.

### Phase 3: Concurrent Web Scraping
1. **Session Health Check:** The system verifies if the Playwright authentication state (`.json` cookies) for each platform is valid.
2. **Asynchronous Execution:** The `PipelineCoordinator` spawns parallel threads using Java's `CompletableFuture` for each active platform (Blinkit, Zepto).
3. **Playwright Execution:**
   - A headless Chromium browser context is instantiated per thread.
   - The bot navigates to the platform's search page using the product name.
   - Dynamic UI elements are waited on using `waitForSelector()`.
   - The product name and price are extracted using precise CSS locators (`div.tw-line-clamp-2`, `[data-testid="product-card-price"]`).
4. **Exception Handling:** If a platform blocks the scraper (e.g., Cloudflare 403 on Zepto) or an item is missing, the scraper catches the exception and flags the item as `isInStock = false`.

### Phase 4: Data Aggregation & Comparison
1. **Data Collation:** Once all `CompletableFuture` threads return, `AggregationService` processes the `ScrapeResult` lists.
2. **Cost Calculation:** It calculates the total price of available items for each platform.
3. **Platform Ranking:** It determines the "Best Option" by checking which platform provides a completely filled cart at the lowest price.
4. **Message Formatting:** A user-friendly string is generated, listing each platform, the status of individual items (price or "❌ Out of stock"), and the final total.

### Phase 5: Outbound Notification
1. **Twilio Client:** The `TwilioOutboundService` takes the formatted string and the original user's phone number.
2. **WhatsApp Dispatch:** It calls the Twilio REST API to send the final comparison message back to the user's WhatsApp.

---

## 💡 SDE-1 Interview Questions based on this Project

If you include this project on your resume, interviewers will likely focus on **Concurrency**, **Database ORM**, **System Design**, and **Resilience**.

### 1. Concurrency & Asynchronous Programming
**Q:** *Your scraping phase uses `CompletableFuture`. Why did you choose this over standard Java Threads or an `ExecutorService`?*
**A:** `CompletableFuture` allows for non-blocking, declarative pipeline execution. It easily enables chaining callbacks and joining multiple asynchronous tasks (e.g., `CompletableFuture.allOf()`) without manually managing `CountDownLatch` or thread join logic. 

**Q:** *What happens if the Blinkit scraper takes 5 seconds, but Zepto hangs indefinitely? How does your system prevent the user from waiting forever?*
**A:** By applying a timeout to the `CompletableFuture` (e.g., `.orTimeout(15, TimeUnit.SECONDS)`) or by using Playwright's built-in `setTimeout` options. If a timeout occurs, the specific task fails gracefully, returning an "Out of stock" fallback, allowing the rest of the pipeline to continue.

### 2. Spring Boot & Database (Hibernate/JPA)
**Q:** *While building this, you encountered a `LazyInitializationException` when trying to access user details in your async pipeline. What caused this and how did you fix it?*
**A:** The main HTTP thread closed the Hibernate database session before handing the `Search` object to the `@Async` background thread. When the async thread tried to read the lazily-loaded `User` entity, the session was gone. I fixed this by using the `@EntityGraph` annotation in the Repository to eagerly fetch the `User` object during the initial database query.

**Q:** *Why did you move the heavy lifting to an `@Async` method instead of returning the scraped prices directly in the Webhook HTTP response?*
**A:** Twilio webhooks require an HTTP 200 OK response within a strict timeframe (usually 15 seconds). Web scraping multiple platforms takes longer. Holding the HTTP connection open would cause Twilio to register a timeout error. Asynchronous processing allows instant acknowledgement while the work happens in the background.

### 3. Resilience & External Integration
**Q:** *Web scraping is inherently flaky because websites change their HTML structure. How did you design your system to handle this?*
**A:** 
1. **Try-Catch Blocks:** Enclosed CSS locator lookups in `try-catch` blocks so a single broken selector doesn't crash the entire pipeline.
2. **Graceful Degradation:** If an item fails to scrape, it's flagged as `isInStock = false` rather than throwing an unhandled exception.
3. **Resilience4j:** (If implemented) Used `@Retry` and `@CircuitBreaker` annotations so transient network failures retry, but persistent blocks trigger the circuit breaker.

**Q:** *How did you handle the OpenAI API rate limits and funding errors?*
**A:** I implemented a Fallback Pattern. If the `OpenAIExtractionService` throws an exception (like a 401 Unauthorized), the `try-catch` block catches it and immediately delegates the task to the `RegexExtractionService`, ensuring the pipeline never breaks for the end user.

### 4. Web Security & Playwright
**Q:** *Why does the bot succeed on Blinkit but fail on Zepto when running locally?*
**A:** Zepto utilizes advanced WAF (Web Application Firewall) like Cloudflare Bot Protection. It detects that the browser is "headless" or automated (e.g., checking `navigator.webdriver`). Blinkit has a more permissible threshold. Overcoming this requires stealth plugins or residential proxies.
