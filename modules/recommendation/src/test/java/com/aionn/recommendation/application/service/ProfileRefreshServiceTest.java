package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.application.port.out.UserAffinityProfilePersistencePort;
import com.aionn.recommendation.domain.model.UserAffinityProfile;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileRefreshServiceTest {

        private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
        private static final String USER_ID = "user-1";
        private static final Duration LOOKBACK = Duration.ofDays(180);

        @Mock
        private InteractionPersistencePort interactionRepository;
        @Mock
        private UserAffinityProfilePersistencePort profileRepository;
        @Mock
        private ProductAttributeQueryPort productAttributeQuery;
        @Mock
        private InteractionWeightPolicy weightPolicy;

        private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        private ProfileRefreshService service() {
                return new ProfileRefreshService(
                                interactionRepository, profileRepository, productAttributeQuery, weightPolicy, clock);
        }

        @Test
        void theStrongestInterestIsRescaledToOne() {
                // Rescaling against the top interest makes a heavy user and a light user with
                // identical tastes
                // directly comparable.
                stubWeights();
                when(interactionRepository.findByUser(eq(USER_ID), any(), anyInt())).thenReturn(List.of(
                                interaction("p-phone", InteractionType.PURCHASE, NOW),
                                interaction("p-cable", InteractionType.VIEW, NOW)));
                stubAttributes(Map.of(
                                "p-phone", attributes("p-phone", "brand-apple", List.of("cat-phone"), 30_000_000),
                                "p-cable", attributes("p-cable", "brand-anker", List.of("cat-cable"), 200_000)));

                service().refresh(USER_ID, LOOKBACK, 10);

                UserAffinityProfile saved = captureSaved();
                assertThat(saved.categoryAffinity("cat-phone").value())
                                .isEqualByComparingTo(BigDecimal.ONE);
                assertThat(saved.categoryAffinity("cat-cable").value()).isLessThan(BigDecimal.ONE);
        }

        @Test
        void olderInteractionsCountForLessThanRecentOnes() {
                stubWeights();
                when(interactionRepository.findByUser(eq(USER_ID), any(), anyInt())).thenReturn(List.of(
                                interaction("p-old", InteractionType.VIEW, NOW.minus(Duration.ofDays(60))),
                                interaction("p-new", InteractionType.VIEW, NOW)));
                stubAttributes(Map.of(
                                "p-old", attributes("p-old", "brand-a", List.of("cat-old"), 1_000_000),
                                "p-new", attributes("p-new", "brand-b", List.of("cat-new"), 1_000_000)));

                service().refresh(USER_ID, LOOKBACK, 10);

                UserAffinityProfile saved = captureSaved();
                assertThat(saved.categoryAffinity("cat-new").value())
                                .isGreaterThan(saved.categoryAffinity("cat-old").value());
        }

        @Test
        void onlyTheTopAffinitiesAreKept() {
                stubWeights();
                when(interactionRepository.findByUser(eq(USER_ID), any(), anyInt())).thenReturn(List.of(
                                interaction("p-1", InteractionType.VIEW, NOW),
                                interaction("p-2", InteractionType.VIEW, NOW),
                                interaction("p-3", InteractionType.VIEW, NOW)));
                stubAttributes(Map.of(
                                "p-1", attributes("p-1", "brand-1", List.of("cat-1"), 1_000_000),
                                "p-2", attributes("p-2", "brand-2", List.of("cat-2"), 1_000_000),
                                "p-3", attributes("p-3", "brand-3", List.of("cat-3"), 1_000_000)));

                service().refresh(USER_ID, LOOKBACK, 2);

                assertThat(captureSaved().getCategoryAffinities()).hasSize(2);
        }

        @Test
        void theBandIgnoresOutliersAtEitherEnd() {
                // p20..p80 rather than min..max, so one unusually cheap or expensive purchase
                // does not stretch
                // the band across the whole catalog.
                stubWeights();
                when(interactionRepository.findByUser(eq(USER_ID), any(), anyInt())).thenReturn(List.of(
                                interaction("p-cheap", InteractionType.VIEW, NOW),
                                interaction("p-mid1", InteractionType.VIEW, NOW),
                                interaction("p-mid2", InteractionType.VIEW, NOW),
                                interaction("p-mid3", InteractionType.VIEW, NOW),
                                interaction("p-luxury", InteractionType.VIEW, NOW)));
                stubAttributes(Map.of(
                                "p-cheap", attributes("p-cheap", "b", List.of("cat-1"), 50_000),
                                "p-mid1", attributes("p-mid1", "b", List.of("cat-1"), 10_000_000),
                                "p-mid2", attributes("p-mid2", "b", List.of("cat-1"), 12_000_000),
                                "p-mid3", attributes("p-mid3", "b", List.of("cat-1"), 15_000_000),
                                "p-luxury", attributes("p-luxury", "b", List.of("cat-1"), 500_000_000)));

                service().refresh(USER_ID, LOOKBACK, 10);

                UserAffinityProfile saved = captureSaved();
                assertThat(saved.getPriceBandMin()).isEqualByComparingTo(BigDecimal.valueOf(10_000_000));
                assertThat(saved.getPriceBandMax()).isEqualByComparingTo(BigDecimal.valueOf(15_000_000));
        }

        @Test
        void historyOfUnpublishedProductsDoesNotShapeTheProfile() {
                stubWeights();
                when(interactionRepository.findByUser(eq(USER_ID), any(), anyInt()))
                                .thenReturn(List.of(interaction("p-gone", InteractionType.PURCHASE, NOW)));
                stubAttributes(Map.of());

                service().refresh(USER_ID, LOOKBACK, 10);

                assertThat(captureSaved().hasNoSignal()).isTrue();
        }

        @Test
        void aUserWithNoInteractionsGetsAnEmptyProfileRatherThanNoRow() {
                // An empty row still records that the refresh ran, so cold-start weighting has
                // something to read.
                when(interactionRepository.findByUser(eq(USER_ID), any(), anyInt())).thenReturn(List.of());

                service().refresh(USER_ID, LOOKBACK, 10);

                assertThat(captureSaved().hasNoSignal()).isTrue();
        }

        @Test
        void profileOfFallsBackToAnEmptyProfileWhenNoneIsStored() {
                when(profileRepository.findByUserId(USER_ID)).thenReturn(java.util.Optional.empty());

                assertThat(service().profileOf(USER_ID).hasNoSignal()).isTrue();
        }

        private void stubWeights() {
                lenient().when(weightPolicy.weightFor(InteractionType.VIEW))
                                .thenReturn(InteractionWeight.of(BigDecimal.ONE, Duration.ofDays(14)));
                lenient().when(weightPolicy.weightFor(InteractionType.CART_ADD))
                                .thenReturn(InteractionWeight.of(BigDecimal.valueOf(4), Duration.ofDays(30)));
                lenient().when(weightPolicy.weightFor(InteractionType.PURCHASE))
                                .thenReturn(InteractionWeight.of(BigDecimal.valueOf(5), Duration.ofDays(180)));
        }

        private void stubAttributes(Map<String, ProductAttributeQueryPort.ProductAttributes> attributes) {
                when(productAttributeQuery.findByProductIds(anyCollection()))
                                .thenReturn(new LinkedHashMap<>(attributes));
        }

        private UserAffinityProfile captureSaved() {
                ArgumentCaptor<UserAffinityProfile> captor = ArgumentCaptor.forClass(UserAffinityProfile.class);
                verify(profileRepository).save(captor.capture(), eq(NOW));
                return captor.getValue();
        }

        private static UserInteraction interaction(
                        String productId, InteractionType type, Instant occurredAt) {
                return new UserInteraction(
                                "int-" + productId, USER_ID, productId, type, BigDecimal.ONE, occurredAt);
        }

        private static ProductAttributeQueryPort.ProductAttributes attributes(
                        String productId, String brandId, Collection<String> categoryIds, long price) {
                return new ProductAttributeQueryPort.ProductAttributes(
                                productId,
                                "Product " + productId,
                                brandId,
                                List.copyOf(categoryIds),
                                List.of("sku-" + productId),
                                null,
                                BigDecimal.valueOf(price),
                                "VND");
        }
}
