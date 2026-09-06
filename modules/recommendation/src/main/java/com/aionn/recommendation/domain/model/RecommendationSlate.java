package com.aionn.recommendation.domain.model;

import com.aionn.recommendation.domain.exception.RecommendationErrorCode;
import com.aionn.recommendation.domain.exception.RecommendationException;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.domain.valueobject.RecommendationReason;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An ordered set of scored recommendations for one surface.
 *
 * <p>Deduplication happens here rather than in each candidate source: the same product legitimately
 * arrives from collaborative filtering, content matching and popularity at once, and the first
 * (highest-scoring) arrival should win along with its reason.
 */
public record RecommendationSlate(RecommendationSurface surface, List<ScoredProduct> items) {

    public RecommendationSlate {
        if (surface == null) {
            throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                    "surface must not be null");
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static RecommendationSlate empty(RecommendationSurface surface) {
        return new RecommendationSlate(surface, List.of());
    }

    /**
     * Sorts by score descending, drops duplicate products, then truncates. Ties break on product ID
     * so a slate is stable across calls - an unstable order makes cached and uncached responses
     * disagree for no reason.
     */
    public static RecommendationSlate of(
            RecommendationSurface surface, List<ScoredProduct> candidates, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return empty(surface);
        }
        List<ScoredProduct> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator
                .comparing(ScoredProduct::score, Comparator.reverseOrder())
                .thenComparing(ScoredProduct::productId));

        Set<String> seen = new LinkedHashSet<>();
        List<ScoredProduct> selected = new ArrayList<>(Math.min(limit, sorted.size()));
        for (ScoredProduct candidate : sorted) {
            if (selected.size() == limit) {
                break;
            }
            if (seen.add(candidate.productId())) {
                selected.add(candidate);
            }
        }
        return new RecommendationSlate(surface, selected);
    }

    public List<String> productIds() {
        return items.stream().map(ScoredProduct::productId).toList();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public record ScoredProduct(String productId, AffinityScore score, RecommendationReason reason) {

        public ScoredProduct {
            if (productId == null || productId.isBlank()) {
                throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                        "productId must not be blank");
            }
            if (score == null) {
                throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                        "score must not be null");
            }
            if (reason == null) {
                throw new RecommendationException(RecommendationErrorCode.INVALID_ARGUMENT,
                        "reason must not be null");
            }
            productId = productId.trim();
        }
    }
}
