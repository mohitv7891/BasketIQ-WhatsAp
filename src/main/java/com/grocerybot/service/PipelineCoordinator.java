package com.grocerybot.service;

import com.grocerybot.dto.CartComparison;
import com.grocerybot.dto.GroceryItemDto;
import com.grocerybot.dto.ScrapeResult;
import com.grocerybot.entity.Search;
import com.grocerybot.repository.SearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PipelineCoordinator {
    private static final Logger log = LoggerFactory.getLogger(PipelineCoordinator.class);

    private final SearchRepository searchRepository;
    private final OpenAiExtractionService openaiExtractionService;
    private final RegexExtractionService regexExtractionService;
    private final SessionHealthService healthService;
    private final PlaywrightScraperService scraperService;
    private final AggregationService aggregationService;
    private final TwilioOutboundService twilioOutboundService;

    public PipelineCoordinator(SearchRepository searchRepository,
                               OpenAiExtractionService openaiExtractionService,
                               RegexExtractionService regexExtractionService,
                               SessionHealthService healthService,
                               PlaywrightScraperService scraperService,
                               AggregationService aggregationService,
                               TwilioOutboundService twilioOutboundService) {
        this.searchRepository = searchRepository;
        this.openaiExtractionService = openaiExtractionService;
        this.regexExtractionService = regexExtractionService;
        this.healthService = healthService;
        this.scraperService = scraperService;
        this.aggregationService = aggregationService;
        this.twilioOutboundService = twilioOutboundService;
    }

    @Async
    public void process(UUID searchId, String rawText) {
        log.info("Starting background processing for searchId: {}", searchId);
        Search search = searchRepository.findById(searchId).orElseThrow();

        try {
            // Phase 2: Session Health
            boolean blinkitHealthy = healthService.isSessionHealthy("blinkit");
            boolean zeptoHealthy = healthService.isSessionHealthy("zepto");

            // Phase 3: Extraction with Fallback
            List<GroceryItemDto> items = regexExtractionService.extractItems(rawText);

            // Phase 4: Scraping
            List<CompletableFuture<ScrapeResult>> scrapeFutures = new ArrayList<>();
            if (blinkitHealthy) {
                scrapeFutures.add(scraperService.scrapePlatformAsync("blinkit", items));
            }
            if (zeptoHealthy) {
                scrapeFutures.add(scraperService.scrapePlatformAsync("zepto", items));
            }

            CompletableFuture.allOf(scrapeFutures.toArray(new CompletableFuture[0])).join();
            List<ScrapeResult> results = scrapeFutures.stream().map(CompletableFuture::join).toList();

            // Phase 5: Aggregation
            CartComparison comparison = aggregationService.aggregate(results);

            // Phase 6: Outbound
            twilioOutboundService.sendMessage(search.getUser().getPhoneNumber(), comparison.getComparisonMessage());

            search.setStatus("COMPLETED");
            searchRepository.save(search);
        } catch (Exception e) {
            log.error("Pipeline failed for searchId: {}", searchId, e);
            search.setStatus("FAILED");
            searchRepository.save(search);
            twilioOutboundService.sendMessage(search.getUser().getPhoneNumber(), "Sorry, I encountered an error while processing your list.");
        }
    }
}
