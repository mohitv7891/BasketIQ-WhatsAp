package com.grocerybot.service;

import com.grocerybot.entity.PriceCache;
import com.grocerybot.repository.PriceCacheRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class PriceCacheService {
    
    private final PriceCacheRepository repository;

    public PriceCacheService(PriceCacheRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "priceCache", key = "#normalizedName + '-' + #quantity + '-' + #unit + '-' + #pincode + '-' + #platform")
    public Optional<PriceCache> getCachedPrice(String normalizedName, BigDecimal quantity, String unit, String pincode, String platform) {
        Optional<PriceCache> cache = repository.findByNormalizedNameAndQuantityAndUnitAndPincodeAndPlatform(
            normalizedName, quantity, unit, pincode, platform
        );
        
        if (cache.isPresent() && cache.get().getFetchedAt().isAfter(OffsetDateTime.now().minusMinutes(10))) {
            return cache;
        }
        
        return Optional.empty();
    }
    
    public void saveCache(String normalizedName, BigDecimal quantity, String unit, String pincode, String platform, BigDecimal price, boolean inStock) {
        // Normally we'd check if it exists first to update, but assuming unique constraints or simple insert logic for Phase 1
        Optional<PriceCache> existing = repository.findByNormalizedNameAndQuantityAndUnitAndPincodeAndPlatform(
            normalizedName, quantity, unit, pincode, platform
        );
        
        PriceCache cache = existing.orElse(new PriceCache());
        cache.setNormalizedName(normalizedName);
        cache.setQuantity(quantity);
        cache.setUnit(unit);
        cache.setPincode(pincode);
        cache.setPlatform(platform);
        cache.setPrice(price);
        cache.setInStock(inStock);
        
        repository.save(cache);
    }
}
