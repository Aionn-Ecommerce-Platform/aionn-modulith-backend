package com.aionn.recommendation.application.service;

import java.util.Objects;

import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.application.port.out.UserAffinityProfilePersistencePort;
import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.domain.valueobject.InteractionType;
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
 * Rebuilds one user's affinity profile from their interaction history. Rebuilt wholesale because
 * every stored score decays continuously; incremental updates would freeze older scores.
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
        // The sweep reads its batch before processing, so an account can be erased meanwhile. Lock, then
        // check under it, or this method writes back the profile erasure just deleted.
        interactionRepository.lockUser(userId);
        if (interactionRepository.isUserErased(userId)) {
            log.debug("Skipping profile refresh for an erased account");
            return;
        }

        List<UserInteraction> interactions = interactionRepository.findByUser(
                userId, now.minus(lookback), Integer.MAX_VALUE);

        if (interactions.isEmpty()) {
            // Clear an existing profile only; writing one for an unknown user would create personal data.
            if (profileRepository.existsByUserId(userId)) {
                profileRepository.save(UserAffinityProfile.empty(userId), now);
            } else {
                log.debug("No interactions and no stored profile for user; nothing to refresh");
            }
            return;
        }

        profileRepository.save(buildProfile(userId, interactions, now, maxAffinities), now);
    }

    private UserAffinityProfile buildProfile(
            String userId, List<UserInteraction> interactions, Instant now, int maxAffinities) {
        Map<String, ProductAttributeQueryPort.ProductAttributes> attributes =
                productAttributeQuery.findByProductIds(
                        interactions.stream()
                                .map(UserInteraction::getProductId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList());

        Map<String, BigDecimal> categoryWeights = new HashMap<>();
        Map<String, BigDecimal> brandWeights = new HashMap<>();
        List<BigDecimal> prices = new ArrayList<>();
        Instant lastInteractionAt = null;

        for (UserInteraction interaction : interactions) {
            BigDecimal decayed = weightPolicy.weightFor(interaction.getType())
                    .decayedAt(interaction.getOccurredAt(), now);

            // Searches carry their facets directly, so they score categories/brands without a product.
            if (interaction.getType() == InteractionType.SEARCH) {
                for (String categoryId : interaction.getCategoryIds()) {
                    categoryWeights.merge(categoryId, decayed, BigDecimal::add);
                }
                if (interaction.getBrandId() != null) {
                    brandWeights.merge(interaction.getBrandId(), decayed, BigDecimal::add);
                }
            } else {
                ProductAttributeQueryPort.ProductAttributes product =
                        attributes.get(interaction.getProductId());
                if (product == null) {
                    // Deleted/taken-down product: its history must not shape recommendations.
                    continue;
                }
                for (String categoryId : product.categoryIds()) {
                    categoryWeights.merge(categoryId, decayed, BigDecimal::add);
                }
                if (product.brandId() != null) {
                    brandWeights.merge(product.brandId(), decayed, BigDecimal::add);
                }
                if (product.priceFrom() != null) {
                    prices.add(product.priceFrom());
                }
            }
            if (lastInteractionAt == null || interaction.getOccurredAt().isAfter(lastInteractionAt)) {
                lastInteractionAt = interaction.getOccurredAt();
            }
        }

        return new UserAffinityProfile(
                userId,
                normalizeTop(categoryWeights, maxAffinities),
                normalizeTop(brandWeights, maxAffinities),
                percentile(prices, PRICE_BAND_LOWER_PERCENTILE),
                percentile(prices, PRICE_BAND_UPPER_PERCENTILE),
                interactions.size(),
                lastInteractionAt);
    }

    @Transactional(readOnly = true)
    public UserAffinityProfile profileOf(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> UserAffinityProfile.empty(userId));
    }

    /** Rescales against the top affinity so the strongest interest is always 1.0, making users comparable. */
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

    /** p20..p80 rather than min..max so one outlier doesn't stretch the price band across the catalog. */
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
