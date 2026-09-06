package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;
import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.domain.model.UserInteraction;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionIngestServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
    private static final String SKU_ID = "01HZSKU0000000000000000001";

    @Mock
    private InteractionPersistencePort interactionRepository;
    @Mock
    private ProductAttributeQueryPort productAttributeQuery;
    @Mock
    private InteractionWeightPolicy weightPolicy;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private InteractionIngestService service() {
        return new InteractionIngestService(
                interactionRepository, productAttributeQuery, weightPolicy, clock);
    }

    @Test
    void productScopedSignalIsRecordedWithoutACatalogLookup() {
        stubWeight(InteractionType.VIEW, 1.0, 14);

        service().ingestInteraction(RecordInteractionCommand.forProduct(
                "user-1", PRODUCT_ID, InteractionType.VIEW, NOW, "evt-1",
                List.of("cat-phone"), "brand-apple"));

        UserInteraction recorded = captureRecorded();
        assertThat(recorded.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(recorded.getType()).isEqualTo(InteractionType.VIEW);
        verify(productAttributeQuery, never()).findProductIdBySkuId(any());
    }

    @Test
    void skuScopedSignalIsResolvedToItsOwningProduct() {
        stubWeight(InteractionType.PURCHASE, 5.0, 180);
        when(productAttributeQuery.findProductIdBySkuId(SKU_ID)).thenReturn(Optional.of(PRODUCT_ID));

        service().ingestInteraction(RecordInteractionCommand.forSku(
                "user-1", SKU_ID, InteractionType.PURCHASE, NOW, "evt-2"));

        assertThat(captureRecorded().getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void anUnresolvableSkuIsSkippedRatherThanFailingTheEvent() {
        // The SKU may belong to a product catalog has since unpublished. Failing here
        // would make the
        // outbox retry an event that can never succeed.
        when(productAttributeQuery.findProductIdBySkuId(SKU_ID)).thenReturn(Optional.empty());

        service().ingestInteraction(RecordInteractionCommand.forSku(
                "user-1", SKU_ID, InteractionType.CART_ADD, NOW, "evt-3"));

        verify(interactionRepository, never()).append(any());
    }

    @Test
    void anonymousActivityIsNotAttributedToAnyone() {
        // Spring Security names an unauthenticated principal "anonymousUser"; storing
        // it would merge
        // every guest's behaviour into one shared profile.
        service().ingestInteraction(RecordInteractionCommand.forProduct(
                "anonymousUser", PRODUCT_ID, InteractionType.VIEW, NOW, "evt-4", List.of(), null));

        verify(interactionRepository, never()).append(any());
    }

    @Test
    void blankAndNullUsersAreIgnored() {
        service().ingestInteraction(RecordInteractionCommand.forProduct(
                null, PRODUCT_ID, InteractionType.VIEW, NOW, "evt-5", List.of(), null));
        service().ingestInteraction(RecordInteractionCommand.forProduct(
                "  ", PRODUCT_ID, InteractionType.VIEW, NOW, "evt-6", List.of(), null));

        verify(interactionRepository, never()).append(any());
    }

    @Test
    void theBaseWeightIsStoredSoDecayCanBeAppliedAtReadTime() {
        // Storing an already-decayed value would freeze it at write time and be wrong
        // within days.
        stubWeight(InteractionType.CART_ADD, 4.0, 30);
        when(productAttributeQuery.findProductIdBySkuId(SKU_ID)).thenReturn(Optional.of(PRODUCT_ID));

        service().ingestInteraction(RecordInteractionCommand.forSku(
                "user-1", SKU_ID, InteractionType.CART_ADD, NOW.minus(Duration.ofDays(60)), "evt-7"));

        assertThat(captureRecorded().getWeight()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
    }

    @Test
    void aMissingEventTimestampFallsBackToTheClock() {
        stubWeight(InteractionType.VIEW, 1.0, 14);

        service().ingestInteraction(RecordInteractionCommand.forProduct(
                "user-1", PRODUCT_ID, InteractionType.VIEW, null, "evt-8", List.of(), null));

        assertThat(captureRecorded().getOccurredAt()).isEqualTo(NOW);
    }

    private void stubWeight(InteractionType type, double base, long halfLifeDays) {
        lenient().when(weightPolicy.weightFor(type)).thenReturn(InteractionWeight.of(
                BigDecimal.valueOf(base), Duration.ofDays(halfLifeDays)));
    }

    private UserInteraction captureRecorded() {
        ArgumentCaptor<UserInteraction> captor = ArgumentCaptor.forClass(UserInteraction.class);
        verify(interactionRepository).append(captor.capture());
        return captor.getValue();
    }
}
