package com.grocerybot.repository;

import com.grocerybot.entity.PriceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

public interface PriceCacheRepository extends JpaRepository<PriceCache, UUID> {
    Optional<PriceCache> findByNormalizedNameAndQuantityAndUnitAndPincodeAndPlatform(
        String normalizedName, BigDecimal quantity, String unit, String pincode, String platform
    );
}
