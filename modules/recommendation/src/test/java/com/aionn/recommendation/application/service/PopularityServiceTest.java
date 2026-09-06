package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.PopularityPersistencePort;
import com.aionn.recommendation.domain.model.ProductPopularity;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.domain.valueobject.InteractionWeight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularityServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @Mock private InteractionPersistencePort interactionRepository;
    @Mock private PopularityPersistencePort popularityRepository;
    @Mock private InteractionWeightPolicy weightPolicy;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private PopularityService service() {
        return new PopularityService(
                interactionRepository, popularityRepository, weightPolicy, clock);
    }

    @Test
    void aggregatedScoresAreWrittenForEveryActiveProduct() {
        stubHalfLives();
        when(interactionRepository.aggregatePopularity(any(), eq(NOW), anyMap())).thenReturn(Map.of(
                "p-1", aggregate(120.5, 100, 20),
                "p-2", aggregate(3.0, 3, 0)));

        int refreshed = service().refresh(Duration.ofDays(30));

        assertThat(refreshed).isEqualTo(2);
        List<ProductPopularity> written = capturePopularities();
        assertThat(written).extracting(ProductPopularity::productId)
                .containsExactlyInAnyOrder("p-1", "p-2");
        assertThat(written).allSatisfy(popularity ->
                assertThat(popularity.computedAt()).isEqualTo(NOW));
    }

    @Test
    void theLookbackWindowStartsAtNowMinusTheGivenDuration() {
        stubHalfLives();
        when(interactionRepository.aggregatePopularity(any(), any(), anyMap())).thenReturn(Map.of());

        service().refresh(Duration.ofDays(30));

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(interactionRepository).aggregatePopularity(since.capture(), eq(NOW), anyMap());
        assertThat(since.getValue()).isEqualTo(NOW.minus(Duration.ofDays(30)));
    }

    @Test
    void productsWithNoRecentActivityLoseTheirRow() {
        // Momentum is genuinely zero for them; leaving a stale row would let them outrank products that
        // are actually selling now.
        stubHalfLives();
        when(interactionRepository.aggregatePopularity(any(), eq(NOW), anyMap()))
                .thenReturn(Map.of("p-1", aggregate(10.0, 10, 0)));

        service().refresh(Duration.ofDays(30));

        verify(popularityRepository).deleteComputedBefore(NOW);
    }

    @Test
    void anEmptyWindowLeavesPreviousScoresUntouched() {
        // Wiping every score because one window happened to be quiet would empty the home feed for all
        // users, so the previous run's data is deliberately kept.
        stubHalfLives();
        when(interactionRepository.aggregatePopularity(any(), eq(NOW), anyMap())).thenReturn(Map.of());

        assertThat(service().refresh(Duration.ofDays(30))).isZero();
        verify(popularityRepository, never()).upsertAll(any());
        verify(popularityRepository, never()).deleteComputedBefore(any());
    }

    @Test
    void everyInteractionTypeContributesItsOwnHalfLife() {
        stubHalfLives();
        when(interactionRepository.aggregatePopularity(any(), eq(NOW), anyMap())).thenReturn(Map.of());

        service().refresh(Duration.ofDays(30));

        ArgumentCaptor<Map<InteractionType, Long>> halfLives = ArgumentCaptor.captor();
        verify(interactionRepository).aggregatePopularity(any(), eq(NOW), halfLives.capture());
        assertThat(halfLives.getValue()).containsOnlyKeys(InteractionType.values());
    }

    private void stubHalfLives() {
        lenient().when(weightPolicy.weightFor(InteractionType.VIEW))
                .thenReturn(InteractionWeight.of(BigDecimal.ONE, Duration.ofDays(14)));
        lenient().when(weightPolicy.weightFor(InteractionType.CART_ADD))
                .thenReturn(InteractionWeight.of(BigDecimal.valueOf(4), Duration.ofDays(30)));
        lenient().when(weightPolicy.weightFor(InteractionType.PURCHASE))
                .thenReturn(InteractionWeight.of(BigDecimal.valueOf(5), Duration.ofDays(180)));
    }

    private List<ProductPopularity> capturePopularities() {
        ArgumentCaptor<List<ProductPopularity>> captor = ArgumentCaptor.captor();
        verify(popularityRepository).upsertAll(captor.capture());
        return captor.getValue();
    }

    private static InteractionPersistencePort.PopularityAggregate aggregate(
            double score, long views, long purchases) {
        return new InteractionPersistencePort.PopularityAggregate(
                BigDecimal.valueOf(score), views, purchases);
    }
}
