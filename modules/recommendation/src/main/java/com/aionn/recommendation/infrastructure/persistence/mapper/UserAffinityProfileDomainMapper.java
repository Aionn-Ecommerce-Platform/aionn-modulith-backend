package com.aionn.recommendation.infrastructure.persistence.mapper;

import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.domain.valueobject.AffinityScore;
import com.aionn.recommendation.infrastructure.persistence.entity.UserAffinityProfileEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class UserAffinityProfileDomainMapper {

    public UserAffinityProfile toDomain(UserAffinityProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserAffinityProfile(
                entity.getUserId(),
                toScores(entity.getCategoryAffinities()),
                toScores(entity.getBrandAffinities()),
                entity.getPriceBandMin(),
                entity.getPriceBandMax(),
                entity.getInteractionCount(),
                entity.getLastInteractionAt());
    }

    public UserAffinityProfileEntity toEntity(UserAffinityProfile domain, Instant refreshedAt) {
        if (domain == null) {
            return null;
        }
        return UserAffinityProfileEntity.builder()
                .userId(domain.getUserId())
                .categoryAffinities(toDecimals(domain.getCategoryAffinities()))
                .brandAffinities(toDecimals(domain.getBrandAffinities()))
                .priceBandMin(domain.getPriceBandMin())
                .priceBandMax(domain.getPriceBandMax())
                .interactionCount(domain.getInteractionCount())
                .lastInteractionAt(domain.getLastInteractionAt())
                .refreshedAt(refreshedAt)
                .build();
    }

    private static Map<String, AffinityScore> toScores(Map<String, BigDecimal> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, AffinityScore> scores = LinkedHashMap.newLinkedHashMap(source.size());
        source.forEach((key, value) -> scores.put(key, AffinityScore.of(value)));
        return scores;
    }

    private static Map<String, BigDecimal> toDecimals(Map<String, AffinityScore> source) {
        if (source == null || source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, BigDecimal> decimals = LinkedHashMap.newLinkedHashMap(source.size());
        source.forEach((key, score) -> decimals.put(key, score.value()));
        return decimals;
    }
}
