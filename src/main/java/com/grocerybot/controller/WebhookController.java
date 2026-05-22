package com.grocerybot.controller;

import com.grocerybot.entity.Search;
import com.grocerybot.entity.User;
import com.grocerybot.repository.SearchRepository;
import com.grocerybot.repository.UserRepository;
import com.grocerybot.service.IdempotencyService;
import com.grocerybot.service.PipelineCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class WebhookController {
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;
    private final SearchRepository searchRepository;
    private final PipelineCoordinator pipelineCoordinator;

    public WebhookController(IdempotencyService idempotencyService,
                             UserRepository userRepository,
                             SearchRepository searchRepository,
                             PipelineCoordinator pipelineCoordinator) {
        this.idempotencyService = idempotencyService;
        this.userRepository = userRepository;
        this.searchRepository = searchRepository;
        this.pipelineCoordinator = pipelineCoordinator;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String handleWebhook(@RequestParam Map<String, String> payload) {
        String messageSid = payload.get("MessageSid");
        String from = payload.get("From");
        String body = payload.get("Body");

        log.info("Received webhook. SID: {}, From: {}, Body: {}", messageSid, from, body);

        if (messageSid == null || idempotencyService.isDuplicate(messageSid)) {
            log.info("Duplicate or invalid message SID: {}. Ignoring.", messageSid);
            return emptyTwiML();
        }

        if (from != null && from.startsWith("whatsapp:")) {
            from = from.replace("whatsapp:", "");
        }

        // Upsert User
        String finalFrom = from;
        User user = userRepository.findByPhoneNumber(finalFrom)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setPhoneNumber(finalFrom);
                    return userRepository.save(newUser);
                });

        // Create Search
        Search search = new Search();
        search.setUser(user);
        search.setRawText(body);
        search.setStatus("PENDING");
        search = searchRepository.save(search);

        // Async Processing
        pipelineCoordinator.process(search.getId(), body);

        // Immediate Ack
        return ackTwiML();
    }

    private String ackTwiML() {
        return "<Response><Message>🛒 Got it! Checking Blinkit &amp; Zepto prices. Back in ~60s...</Message></Response>";
    }

    private String emptyTwiML() {
        return "<Response></Response>";
    }
}
