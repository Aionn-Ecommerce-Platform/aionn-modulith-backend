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

    private static final int MIN_CO_OCCURRENCE = 2;
    private static final int MAX_NEIGHBOURS = 50;

    @Mock private InteractionPersistencePort interactionRepository;
    @Mock private ItemSimilarityPersistencePort similarityRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private ItemSimilarityService service() {
        return new ItemSimilarityService(interactionRepository, similarityRepository, clock);
    }

    private int refresh(Duration lookback) {
        return service().refresh(lookback, MIN_CO_OCCURRENCE, MAX_NEIGHBOURS);
    }

    @Test
    void eachPairIsStoredInBothDirections() {
        // Storing both directions lets a neighbour lookup use one indexed predicate instead of an OR
        // across two columns.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(row("p-1", "p-2", 0.71, 5)));

        int pairs = refresh(Duration.ofDays(180));

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
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of());

        refresh(Duration.ofDays(180));

        ArgumentCaptor<Collection<InteractionType>> types = ArgumentCaptor.captor();
        verify(interactionRepository)
                .computeItemSimilarity(any(), types.capture(), eq(MIN_CO_OCCURRENCE), eq(MAX_NEIGHBOURS));
        assertThat(types.getValue())
                .containsExactlyInAnyOrder(InteractionType.CART_ADD, InteractionType.PURCHASE)
                .doesNotContain(InteractionType.VIEW);
    }

    @Test
    void pairsMissingFromTheLatestRunAreRemoved() {
        // Otherwise a single co-purchase from months ago would keep recommending forever.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(row("p-1", "p-2", 0.5, 3)));

        refresh(Duration.ofDays(180));

        verify(similarityRepository).deleteComputedBefore(NOW);
    }

    @Test
    void noQualifyingPairsStillSweepsTheStaleMatrix() {
        // An empty result is a statement that nothing co-occurs any more, not a reason to leave the
        // previous matrix in place: pairs that dropped below the threshold would otherwise keep
        // recommending each other until some later run happens to produce a non-empty result.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of());

        assertThat(refresh(Duration.ofDays(180))).isZero();
        verify(similarityRepository, never()).upsertAll(any());
        verify(similarityRepository).deleteComputedBefore(NOW);
    }

    @Test
    void aCosineSlightlyAboveOneIsClampedRatherThanRejected() {
        // Floating-point cosine can land a hair outside [0,1]; the excess is at rounding scale, so
        // clamping keeps the value object's invariant without masking a real error.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(row("p-1", "p-2", 1.0000000002, 4)));

        refresh(Duration.ofDays(180));

        assertThat(captureWritten().getFirst().score().value())
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void theLookbackWindowIsRelativeToTheInjectedClock() {
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of());

        refresh(Duration.ofDays(90));

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(interactionRepository)
                .computeItemSimilarity(since.capture(), anyCollection(), anyInt(), anyInt());
        assertThat(since.getValue()).isEqualTo(NOW.minus(Duration.ofDays(90)));
    }

    @Test
    void theNeighbourCapReachesTheQuery() {
        // The cap is the only thing bounding the stored matrix, so a refresh that drops it would let a
        // popular product accumulate neighbours no reader will ever page through.
        when(interactionRepository.computeItemSimilarity(any(), anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of());

        service().refresh(Duration.ofDays(180), 7, 12);

        verify(interactionRepository).computeItemSimilarity(any(), anyCollection(), eq(7), eq(12));
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
