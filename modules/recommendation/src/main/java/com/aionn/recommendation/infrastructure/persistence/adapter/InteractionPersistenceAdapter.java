package com.aionn.recommendation.infrastructure.persistence.adapter;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.infrastructure.persistence.mapper.InteractionDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.InteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InteractionPersistenceAdapter implements InteractionPersistencePort {

    /**
     * Postgres caps bind parameters at 65535; well under it, and far more than any
     * real user has.
     */
    private static final int MAX_USER_INTERACTIONS = 10_000;

    private final InteractionRepository jpa;
    private final InteractionDomainMapper mapper;
    private final Clock clock;

    @Override
    public void append(UserInteraction interaction) {
        jpa.save(mapper.toEntity(interaction, clock.instant()));
    }

    @Override
    public List<UserInteraction> findByUser(String userId, Instant since, int limit) {
        int safeLimit = limit <= 0 || limit > MAX_USER_INTERACTIONS ? MAX_USER_INTERACTIONS : limit;
        return jpa.findByUserSince(userId, since, safeLimit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<String> findPurchasedProductIds(String userId) {
        return jpa.findPurchasedProductIds(userId);
    }

    @Override
    public List<String> findUserIdsWithInteractionsSince(Instant since, int limit) {
        return jpa.findUserIdsWithInteractionsSince(since, Math.max(1, limit));
    }

    @Override
    public Map<String, PopularityAggregate> aggregatePopularity(
            Instant since, Instant now, Map<InteractionType, Long> halfLifeSeconds) {

        List<InteractionRepository.PopularityProjection> rows = jpa.aggregatePopularity(
                since,
                now,
                halfLifeSeconds.get(InteractionType.VIEW),
                halfLifeSeconds.get(InteractionType.CART_ADD),
                halfLifeSeconds.get(InteractionType.PURCHASE));

        Map<String, PopularityAggregate> aggregates = LinkedHashMap.newLinkedHashMap(rows.size());
        for (InteractionRepository.PopularityProjection row : rows) {
            BigDecimal score = row.getDecayedScore() == null ? BigDecimal.ZERO : row.getDecayedScore();
            aggregates.put(row.getProductId(), new PopularityAggregate(
                    score, row.getViewCount(), row.getPurchaseCount()));
        }
        return aggregates;
    }

    @Override
    public List<SimilarityRow> computeItemSimilarity(
            Instant since, Collection<InteractionType> strongTypes, int minCoOccurrence) {
        if (strongTypes == null || strongTypes.isEmpty()) {
            return List.of();
        }
        List<String> typeNames = strongTypes.stream().map(InteractionType::name).toList();
        List<InteractionRepository.SimilarityProjection> rows = jpa.computeItemSimilarity(since, typeNames,
                Math.max(1, minCoOccurrence));

        List<SimilarityRow> similarities = new ArrayList<>(rows.size());
        for (InteractionRepository.SimilarityProjection row : rows) {
            similarities.add(new SimilarityRow(
                    row.getProductId(), row.getSimilarProductId(), row.getScore(), row.getCoOccurrence()));
        }
        return similarities;
    }

    @Override
    public int deleteOlderThan(Instant cutoff, int batchSize) {
        return jpa.deleteOlderThan(cutoff, Math.max(1, batchSize));
    }

    @Override
    public int deleteByUser(String userId) {
        return jpa.deleteByUserId(userId);
    }
}
