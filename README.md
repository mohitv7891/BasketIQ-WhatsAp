# Grocery Bot

A WhatsApp bot to compare grocery prices between Blinkit and Zepto in Bengaluru.

## Architecture & Stack
- **Backend**: Java 21, Spring Boot 3.3
- **Scraping**: Playwright Java (Headless Chromium)
- **AI**: Anthropic Claude (`claude-3-5-sonnet-20240620`)
- **Database**: PostgreSQL 16
- **Messaging**: Twilio WhatsApp API

## Local Development Setup

### Prerequisites
1. Docker Desktop
2. Java 21
3. Maven

### Steps

1. **Database Setup**
   ```bash
   docker compose up -d
   ```

2. **Environment Variables**
   Create a `.env` file in the root or export the following variables:
   ```bash
   export TWILIO_ACCOUNT_SID=your_sid
   export TWILIO_AUTH_TOKEN=your_token
   export TWILIO_FROM_NUMBER=whatsapp:+14155238886
   export TWILIO_OWNER_NUMBER=whatsapp:+919876543210
   export CLAUDE_API_KEY=your_claude_key
   ```

3. **Session Authentication (Manual)**
   You must save the authentication state for both platforms before running the bot.
   ```bash
   mvn clean install -DskipTests
   mvn exec:java -Dexec.mainClass=com.grocerybot.tools.SaveSession -Dexec.args="blinkit"
   # Follow instructions in the opened browser to login
   
   mvn exec:java -Dexec.mainClass=com.grocerybot.tools.SaveSession -Dexec.args="zepto"
   # Follow instructions in the opened browser to login
   ```
   This generates `auth/blinkit-auth.json` and `auth/zepto-auth.json`.

4. **Run Application**
   ```bash
   mvn spring-boot:run
   ```

5. **Expose Localhost (ngrok)**
   ```bash
   ngrok http 8080
   ```
   Copy the HTTPS ngrok URL and paste it into your Twilio Sandbox Webhook configuration: `https://<ngrok-url>/webhook`.

## Usage
Send a WhatsApp message to your Twilio number:
> "Hey, I need 1L milk, 500g paneer, and 2 pcs bread."
