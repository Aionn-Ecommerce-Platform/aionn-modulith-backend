package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.application.port.out.UserAffinityProfilePersistencePort;
import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds one user's affinity profile from their interaction history.
 *
 * <p>Rebuilt wholesale rather than updated per interaction because every stored score decays
 * continuously: an incremental update would leave older scores frozen at the value they had when they
 * were last touched.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileRefreshService {

    private static final int PRICE_BAND_LOWER_PERCENTILE = 20;
    private static final int PRICE_BAND_UPPER_PERCENTILE = 80;

    private final InteractionPersistencePort interactionRepository;
    private final UserAffinityProfilePersistencePort profileRepository;
    private final ProductAttributeQueryPort productAttributeQuery;
    private final InteractionWeightPolicy weightPolicy;
    private final Clock clock;

    @Transactional
    public void refresh(String userId, Duration lookback, int maxAffinities) {
        Instant now = clock.instant();
        List<UserInteraction> interactions = interactionRepository.findByUser(
                userId, now.minus(lookback), Integer.MAX_VALUE);

        if (interactions.isEmpty()) {
            profileRepository.save(UserAffinityProfile.empty(userId), now);
            return;
        }

        Map<String, ProductAttributeQueryPort.ProductAttributes> attributes =
                productAttributeQuery.findByProductIds(
                        interactions.stream().map(UserInteraction::getProductId).distinct().toList());

        Map<String, BigDecimal> categoryWeights = new HashMap<>();
        Map<String, BigDecimal> brandWeights = new HashMap<>();
        List<BigDecimal> prices = new ArrayList<>();
        Instant lastInteractionAt = null;

        for (UserInteraction interaction : interactions) {
            ProductAttributeQueryPort.ProductAttributes product =
                    attributes.get(interaction.getProductId());
            if (product == null) {
                // Product was deleted or taken down; its history should not shape recommendations.
                continue;
            }
            BigDecimal decayed = weightPolicy.weightFor(interaction.getType())
                    .decayedAt(interaction.getOccurredAt(), now);

            for (String categoryId : product.categoryIds()) {
                categoryWeights.merge(categoryId, decayed, BigDecimal::add);
            }
            if (product.brandId() != null) {
                brandWeights.merge(product.brandId(), decayed, BigDecimal::add);
            }
            if (product.priceFrom() != null) {
                prices.add(product.priceFrom());
            }
            if (lastInteractionAt == null || interaction.getOccurredAt().isAfter(lastInteractionAt)) {
                lastInteractionAt = interaction.getOccurredAt();
            }
        }

        UserAffinityProfile profile = new UserAffinityProfile(
                userId,
                normalizeTop(categoryWeights, maxAffinities),
                normalizeTop(brandWeights, maxAffinities),
                percentile(prices, PRICE_BAND_LOWER_PERCENTILE),
                percentile(prices, PRICE_BAND_UPPER_PERCENTILE),
                interactions.size(),
                lastInteractionAt);

        profileRepository.save(profile, now);
    }

    @Transactional(readOnly = true)
    public UserAffinityProfile profileOf(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> UserAffinityProfile.empty(userId));
    }

    /**
     * Keeps the strongest affinities and rescales them against the highest one, so the top interest
     * always reads as 1.0 regardless of how much raw activity the user has. Without that, a heavy user
     * and a light user with identical tastes would produce incomparable profiles.
     */
    private static Map<String, AffinityScore> normalizeTop(Map<String, BigDecimal> weights, int limit) {
        if (weights.isEmpty()) {
            return Map.of();
        }
        List<Map.Entry<String, BigDecimal>> ranked = new ArrayList<>(weights.entrySet());
        ranked.sort(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));

        BigDecimal max = ranked.get(0).getValue();
        if (max.signum() <= 0) {
            return Map.of();
        }
        Map<String, AffinityScore> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : ranked.subList(0, Math.min(limit, ranked.size()))) {
            normalized.put(entry.getKey(),
                    AffinityScore.normalize(entry.getValue(), BigDecimal.ZERO, max));
        }
        return normalized;
    }

    /**
     * Percentile over the prices the user actually engaged with. p20..p80 rather than min..max so one
     * unusually cheap or expensive product does not stretch the band to cover the whole catalog.
     */
    private static BigDecimal percentile(List<BigDecimal> values, int percentile) {
        if (values.isEmpty()) {
            return null;
        }
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int index = (int) Math.round((percentile / 100.0d) * (sorted.size() - 1));
        return sorted.get(Math.clamp(index, 0, sorted.size() - 1));
    }
}
