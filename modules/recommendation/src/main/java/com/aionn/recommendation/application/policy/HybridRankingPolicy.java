package com.aionn.recommendation.application.policy;

import com.aionn.recommendation.domain.model.RecommendationSlate;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.domain.valueobject.RecommendationReason;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines the three candidate signals into one ranked list.
 * Signals are min-max normalised before weighting, otherwise raw popularity would dominate.
 */
@Component
public class HybridRankingPolicy {

    private static final int SCORE_SCALE = 5;

    public RecommendationSlate rank(
            RecommendationSurface surface,
            SignalScores signals,
            ColdStartPolicy.SignalWeights weights,
            Collection<String> excludedProductIds,
            int limit) {

        BigDecimal weightSum = weights.collaborative()
                .add(weights.content())
                .add(weights.popularity());
        if (weightSum.signum() <= 0) {
            return RecommendationSlate.empty(surface);
        }

        Map<String, BigDecimal> collaborative = normalize(signals.collaborative());
        Map<String, BigDecimal> content = normalize(signals.content());
        Map<String, BigDecimal> popularity = normalize(signals.popularity());

        Map<String, BigDecimal> combined = new LinkedHashMap<>();
        accumulate(combined, collaborative, weights.collaborative());
        accumulate(combined, content, weights.content());
        accumulate(combined, popularity, weights.popularity());

        List<RecommendationSlate.ScoredProduct> scored = new ArrayList<>(combined.size());
        for (Map.Entry<String, BigDecimal> entry : combined.entrySet()) {
            String productId = entry.getKey();
            if (excludedProductIds != null && excludedProductIds.contains(productId)) {
                continue;
            }
            BigDecimal finalScore = entry.getValue()
                    .divide(weightSum, SCORE_SCALE, RoundingMode.HALF_UP)
                    .min(BigDecimal.ONE);
            scored.add(new RecommendationSlate.ScoredProduct(
                    productId,
                    AffinityScore.of(finalScore),
                    dominantReason(surface, productId, collaborative, content, popularity, weights)));
        }
        return RecommendationSlate.of(surface, scored, limit);
    }

    private static void accumulate(
            Map<String, BigDecimal> target, Map<String, BigDecimal> source, BigDecimal weight) {
        if (weight.signum() <= 0) {
            return;
        }
        source.forEach((productId, score) -> target.merge(productId, score.multiply(weight), BigDecimal::add));
    }

    /** Attributes the recommendation to the signal that contributed most; also-bought gets a specific label. */
    private static RecommendationReason dominantReason(
            RecommendationSurface surface,
            String productId,
            Map<String, BigDecimal> collaborative,
            Map<String, BigDecimal> content,
            Map<String, BigDecimal> popularity,
            ColdStartPolicy.SignalWeights weights) {

        if (surface == RecommendationSurface.SIMILAR && collaborative.containsKey(productId)) {
            return RecommendationReason.SIMILAR_TO_VIEWED;
        }
        if (surface == RecommendationSurface.ALSO_BOUGHT && collaborative.containsKey(productId)) {
            return RecommendationReason.FREQUENTLY_BOUGHT_TOGETHER;
        }

        BigDecimal cf = contribution(collaborative, productId, weights.collaborative());
        BigDecimal ct = contribution(content, productId, weights.content());
        BigDecimal pop = contribution(popularity, productId, weights.popularity());

        if (cf.compareTo(ct) >= 0 && cf.compareTo(pop) >= 0 && cf.signum() > 0) {
            return surface == RecommendationSurface.ALSO_BOUGHT
                    ? RecommendationReason.FREQUENTLY_BOUGHT_TOGETHER
                    : RecommendationReason.SIMILAR_TO_VIEWED;
        }
        if (ct.compareTo(pop) >= 0 && ct.signum() > 0) {
            return RecommendationReason.MATCHES_YOUR_INTERESTS;
        }
        return RecommendationReason.TRENDING;
    }

    private static BigDecimal contribution(
            Map<String, BigDecimal> scores, String productId, BigDecimal weight) {
        return scores.getOrDefault(productId, BigDecimal.ZERO).multiply(weight);
    }

    /**
     * Min-max normalisation. A constant signal maps to one so a selected product still reports a
     * meaningful score; relative ordering is unaffected.
     */
    private static Map<String, BigDecimal> normalize(Map<String, BigDecimal> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        BigDecimal min = null;
        BigDecimal max = null;
        for (BigDecimal value : raw.values()) {
            if (value == null) {
                continue;
            }
            min = min == null ? value : min.min(value);
            max = max == null ? value : max.max(value);
        }
        if (min == null) {
            return Map.of();
        }
        BigDecimal range = max.subtract(min);
        Map<String, BigDecimal> normalized = LinkedHashMap.newLinkedHashMap(raw.size());
        for (Map.Entry<String, BigDecimal> entry : raw.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            BigDecimal value = range.signum() <= 0
                    ? BigDecimal.ONE
                    : entry.getValue().subtract(min).divide(range, SCORE_SCALE, RoundingMode.HALF_UP);
            normalized.put(entry.getKey(), value);
        }
        return normalized;
    }

    /**
     * Raw, un-normalised scores keyed by product ID, one map per candidate source.
     */
    public record SignalScores(
            Map<String, BigDecimal> collaborative,
            Map<String, BigDecimal> content,
            Map<String, BigDecimal> popularity) {

        public SignalScores {
            collaborative = collaborative == null ? Map.of() : Map.copyOf(collaborative);
            content = content == null ? Map.of() : Map.copyOf(content);
            popularity = popularity == null ? Map.of() : Map.copyOf(popularity);
        }
    }
}
