# BasketIQ: Grocery Bot

A WhatsApp bot to automatically compare grocery prices between Blinkit and Zepto. Send your grocery list via WhatsApp, and the bot will concurrently scrape platforms to find you the best and cheapest complete cart.

## 🏗 Architecture & Stack
- **Backend**: Java 17, Spring Boot 3.3
- **Scraping**: Playwright Java (Headless Chromium) with concurrent isolated contexts
- **AI Extraction**: OpenAI (`gpt-5.4-mini`) with automatic fallback to Regex-based extraction
- **Database**: PostgreSQL 16 (running on port 5433)
- **Messaging**: Twilio WhatsApp API
- **Concurrency**: Asynchronous pipeline using `CompletableFuture`

## 🚀 Local Development Setup

### Prerequisites
1. Docker Desktop
2. Java 17
3. Maven

### Steps

1. **Database Setup**
   ```bash
   docker compose up -d
   ```
   *(Note: The PostgreSQL container runs on port 5433 to avoid conflicts with local DBs).*

2. **Environment Variables**
   Update `src/main/resources/application.yml` or set the following environment variables:
   ```bash
   export TWILIO_ACCOUNT_SID=your_twilio_sid
   export TWILIO_AUTH_TOKEN=your_twilio_token
   export TWILIO_FROM_NUMBER=whatsapp:+14155238886
   export TWILIO_OWNER_NUMBER=whatsapp:+919826403071
   export OPENAI_API_KEY=your_openai_key
   ```

3. **Session Authentication (Manual)**
   Because quick-commerce platforms use bot protection, you must manually log in once to generate session cookies (`auth/*.json`) before running the bot.
   ```bash
   # Compile first
   mvn clean compile
   
   # Generate Blinkit session
   mvn exec:java -Dexec.mainClass=com.grocerybot.tools.SaveSession -Dexec.args="blinkit"
   # Follow instructions in the opened browser to login and close it
   
   # Generate Zepto session
   mvn exec:java -Dexec.mainClass=com.grocerybot.tools.SaveSession -Dexec.args="zepto"
   # Follow instructions in the opened browser to login and close it
   ```

4. **Run Application**
   ```bash
   mvn spring-boot:run
   ```

5. **Expose Localhost (ngrok)**
   ```bash
   ngrok http 8080
   ```
   Copy the HTTPS ngrok URL and paste it into your Twilio Sandbox Webhook configuration: `https://<ngrok-url>/api/webhook`.

## 💬 Usage
Send a WhatsApp message to your Twilio number:
> "1 L milk, 500g paneer, and 2 pcs bread"

The bot will reply instantly acknowledging the request, launch headless Chrome tabs to scrape live prices, aggregate the final carts, and text you back with the itemized breakdown.

## 🛡️ Known Limitations
- **Cloudflare Bot Protection**: Zepto employs aggressive WAF/bot protection that frequently blocks headless Chromium instances. While Blinkit is more permissible, if Zepto blocks the bot, it will gracefully fallback and mark the items as "❌ Out of stock". Overcoming this requires stealth plugins or residential proxies.
