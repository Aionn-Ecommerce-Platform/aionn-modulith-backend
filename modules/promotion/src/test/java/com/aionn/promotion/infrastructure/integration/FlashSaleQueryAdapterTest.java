package com.aionn.promotion.infrastructure.integration;

import com.aionn.promotion.application.port.out.FlashSaleRegistrationPersistencePort;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.infrastructure.persistence.entity.PromotionCampaignEntity;
import com.aionn.promotion.infrastructure.persistence.repository.PromotionCampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashSaleQueryAdapterTest {

    @Mock FlashSaleRegistrationPersistencePort registrationRepository;
    @Mock PromotionCampaignPersistencePort campaignRepository;
    @Mock PromotionCampaignRepository campaignJpaRepository;

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
                org.mockito.ArgumentMatchers.eq("FLASH_SALE"),
                org.mockito.ArgumentMatchers.eq("RUNNING"),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(campaign));
        when(registrationRepository.findByCampaign("campaign-1",
                com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus.APPROVED, 200))
                .thenReturn(List.of());

        var result = new FlashSaleQueryAdapter(
                registrationRepository, campaignRepository, campaignJpaRepository)
                .listActiveCampaigns(500);

        assertThat(result).hasSize(1);
        verify(campaignJpaRepository).findByTypeAndStatus(
                org.mockito.ArgumentMatchers.eq("FLASH_SALE"),
                org.mockito.ArgumentMatchers.eq("RUNNING"),
                argThat(page -> page.getPageSize() == 50));
    }
}
