package com.grocerybot.service;

import com.grocerybot.dto.GroceryItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiExtractionService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiExtractionService.class);

    private final SafeJsonParser jsonParser;
    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("classpath:prompts/extract-grocery.txt")
    private Resource promptResource;

    public OpenAiExtractionService(SafeJsonParser jsonParser, WebClient.Builder webClientBuilder) {
        this.jsonParser = jsonParser;
        this.webClient = webClientBuilder.baseUrl("https://api.openai.com/v1").build();
    }

    public List<GroceryItemDto> extractItems(String rawText) throws Exception {
        String promptTemplate = new String(promptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String prompt = promptTemplate.replace("{TEXT}", rawText);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "max_tokens", 1024,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        int maxRetries = 2;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String responseBody = webClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                String textOutput = extractTextFromOpenAiResponse(responseBody);
                return jsonParser.parse(textOutput);
                
            } catch (Exception e) {
                log.warn("Extraction attempt {} failed", i + 1, e);
                if (i == maxRetries - 1) {
                    throw e;
                }
            }
        }
        throw new Exception("Failed to extract grocery items after retries");
    }

    private String extractTextFromOpenAiResponse(String responseJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseJson);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return responseJson;
        }
    }
}
