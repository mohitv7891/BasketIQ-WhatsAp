package com.grocerybot.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class TwilioOutboundService {
    private static final Logger log = LoggerFactory.getLogger(TwilioOutboundService.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (!"stub_sid".equals(accountSid) && accountSid != null && !accountSid.isBlank()) {
            Twilio.init(accountSid, authToken);
        }
    }

    public void sendMessage(String to, String body) {
        if ("stub_sid".equals(accountSid) || accountSid == null || accountSid.isBlank()) {
            log.info("Twilio stub mode. Would have sent message to {}: \n{}", to, body);
            return;
        }

        try {
            // Check length and chunk if needed (> 4000)
            if (body.length() > 3900) {
                // Chunking logic placeholder
                body = body.substring(0, 3900) + "...(truncated)";
            }
            
            Message message = Message.creator(
                    new PhoneNumber("whatsapp:" + to),
                    new PhoneNumber(fromNumber),
                    body
            ).create();
            
            log.info("Sent WhatsApp message SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}", to, e);
        }
    }
}
