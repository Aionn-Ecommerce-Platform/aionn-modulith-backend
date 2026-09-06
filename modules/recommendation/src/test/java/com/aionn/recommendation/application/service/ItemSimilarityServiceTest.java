package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ItemSimilarityPersistencePort;
import com.aionn.recommendation.domain.model.ItemSimilarity;
import com.aionn.recommendation.domain.valueobject.InteractionType;
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
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemSimilarityServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

    @Mock private InteractionPersistencePort interactionRepository;
    @Mock private ItemSimilarityPersistencePort similarityRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private ItemSimilarityService service() {
        return new ItemSimilarityService(interactionRepository, similarityRepository, clock);
    }

    @Test
    void eachPairIsStoredInBothDirections() {
        // Storing both directions lets a neighbour lookup use one indexed predicate instead of an OR
        // across two columns.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt()))
                .thenReturn(List.of(row("p-1", "p-2", 0.71, 5)));

        int pairs = service().refresh(Duration.ofDays(180), 2);

        assertThat(pairs).isEqualTo(1);
        List<ItemSimilarity> written = captureWritten();
        assertThat(written).hasSize(2);
        assertThat(written).extracting(ItemSimilarity::productId)
                .containsExactlyInAnyOrder("p-1", "p-2");
        assertThat(written).extracting(ItemSimilarity::similarProductId)
                .containsExactlyInAnyOrder("p-2", "p-1");
    }

    @Test
    void onlyStrongSignalsFeedTheMatrix() {
        // Views are exploratory: two products seen in one browsing session say far less than two
        // products in one basket.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt()))
                .thenReturn(List.of());

        service().refresh(Duration.ofDays(180), 2);

        ArgumentCaptor<Collection<InteractionType>> types = ArgumentCaptor.captor();
        verify(interactionRepository).computeItemSimilarity(any(), types.capture(), eq(2));
        assertThat(types.getValue())
                .containsExactlyInAnyOrder(InteractionType.CART_ADD, InteractionType.PURCHASE)
                .doesNotContain(InteractionType.VIEW);
    }

    @Test
    void pairsMissingFromTheLatestRunAreRemoved() {
        // Otherwise a single co-purchase from months ago would keep recommending forever.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt()))
                .thenReturn(List.of(row("p-1", "p-2", 0.5, 3)));

        service().refresh(Duration.ofDays(180), 2);

        verify(similarityRepository).deleteComputedBefore(NOW);
    }

    @Test
    void noQualifyingPairsLeavesTheExistingMatrixInPlace() {
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt()))
                .thenReturn(List.of());

        assertThat(service().refresh(Duration.ofDays(180), 2)).isZero();
        verify(similarityRepository, never()).upsertAll(any());
        verify(similarityRepository, never()).deleteComputedBefore(any());
    }

    @Test
    void aCosineSlightlyAboveOneIsClampedRatherThanRejected() {
        // Floating-point cosine can land a hair outside [0,1]; the excess is at rounding scale, so
        // clamping keeps the value object's invariant without masking a real error.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt()))
                .thenReturn(List.of(row("p-1", "p-2", 1.0000000002, 4)));

        service().refresh(Duration.ofDays(180), 2);

        assertThat(captureWritten().getFirst().score().value())
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void theLookbackWindowIsRelativeToTheInjectedClock() {
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt()))
                .thenReturn(List.of());

        service().refresh(Duration.ofDays(90), 2);

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(interactionRepository).computeItemSimilarity(since.capture(), anyCollection(), anyInt());
        assertThat(since.getValue()).isEqualTo(NOW.minus(Duration.ofDays(90)));
    }

    private List<ItemSimilarity> captureWritten() {
        ArgumentCaptor<List<ItemSimilarity>> captor = ArgumentCaptor.captor();
        verify(similarityRepository).upsertAll(captor.capture());
        return captor.getValue();
    }

    private static InteractionPersistencePort.SimilarityRow row(
            String productId, String similarProductId, double score, int coOccurrence) {
        return new InteractionPersistencePort.SimilarityRow(
                productId, similarProductId, score, coOccurrence);
    }
}
