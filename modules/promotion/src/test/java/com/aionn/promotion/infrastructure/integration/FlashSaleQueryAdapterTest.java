package com.aionn.promotion.infrastructure.integration;

import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.contracts.port.promotion.FlashSaleQueryPort;
import org.mockito.ArgumentMatchers;

import com.aionn.promotion.application.port.out.FlashSaleRegistrationPersistencePort;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.policy.PromotionLimitPolicy;
import com.aionn.promotion.infrastructure.persistence.entity.PromotionCampaignEntity;
import com.aionn.promotion.infrastructure.persistence.repository.PromotionCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashSaleQueryAdapterTest {

        @Mock
        FlashSaleRegistrationPersistencePort registrationRepository;
        @Mock
        PromotionCampaignPersistencePort campaignRepository;
        @Mock
        PromotionCampaignRepository campaignJpaRepository;
        @Mock
        JdbcTemplate jdbcTemplate;
        @Mock
        PromotionLimitPolicy limitPolicy;

        @Test
        void activeCampaignLookupFiltersAndLimitsInDatabase() {
                PromotionCampaignEntity campaign = PromotionCampaignEntity.builder()
                                .campaignId("campaign-1")
                                .name("Flash")
                                .type("FLASH_SALE")
                                .status("RUNNING")
                                .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                                .endDate(Instant.parse("2026-01-02T00:00:00Z"))
                                .build();
                when(campaignJpaRepository.findByTypeAndStatus(
                                ArgumentMatchers.eq("FLASH_SALE"),
                                ArgumentMatchers.eq("RUNNING"),
                                ArgumentMatchers.any(Pageable.class)))
                                .thenReturn(List.of(campaign));
                when(registrationRepository.findByCampaign("campaign-1",
                                FlashSaleRegistrationStatus.APPROVED, 200))
                                .thenReturn(List.of());
                org.mockito.Mockito.when(limitPolicy.maxActiveFlashSaleLimit()).thenReturn(50);
                org.mockito.Mockito.when(limitPolicy.maxRegistrationLimit()).thenReturn(200);

                var result = new FlashSaleQueryAdapter(
                                registrationRepository, campaignRepository, campaignJpaRepository, jdbcTemplate,
                                limitPolicy)
                                .listActiveCampaigns(500);

                assertThat(result).hasSize(1);
                verify(campaignJpaRepository).findByTypeAndStatus(
                                ArgumentMatchers.eq("FLASH_SALE"),
                                ArgumentMatchers.eq("RUNNING"),
                                argThat(page -> page.getPageSize() == 50));
        }

        @Test
        void reserveRejectsAnAllocationWhenTheAtomicCapacityUpdateDoesNotMatch() {
                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<ResultSetExtractor<Void>>any(),
                                ArgumentMatchers.<Object>any(), ArgumentMatchers.<Object>any()))
                                .thenReturn(null);
                when(jdbcTemplate.update(anyString(),
                                ArgumentMatchers.<Object>any(), ArgumentMatchers.<Object>any(),
                                ArgumentMatchers.<Object>any()))
                                .thenReturn(0);
                var adapter = new FlashSaleQueryAdapter(
                                registrationRepository, campaignRepository, campaignJpaRepository, jdbcTemplate,
                                limitPolicy);

                assertThatThrownBy(() -> adapter.reserve("order-1", List.of(
                                new FlashSaleQueryPort.Allocation(
                                                "registration-1", 2))))
                                .isInstanceOf(FlashSaleQueryPort.CapacityExceededException.class);
        }
}
