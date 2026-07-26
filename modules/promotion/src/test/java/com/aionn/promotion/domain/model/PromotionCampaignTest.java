package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.event.PromotionEvents;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.valueobject.CampaignStatus;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionCampaignTest {

    private static final String CAMPAIGN_ID = "camp-1";
    private static final String NAME = "Black Friday";
    private static final String USER = "admin-1";
    private static final String CCY = "VND";

    @Test
    void createSuccessAndRecordsEvents() {
        Money budget = Money.of(new BigDecimal("10000000"), CCY);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(5, ChronoUnit.DAYS);

        PromotionCampaign campaign = PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT, budget, start, end, USER);

        assertThat(campaign.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(campaign.getName()).isEqualTo(NAME);
        assertThat(campaign.getType()).isEqualTo(CampaignType.DISCOUNT);
        assertThat(campaign.getBudget()).isEqualTo(budget);
        assertThat(campaign.getBudgetRemaining()).isEqualTo(budget);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SCHEDULED);
        assertThat(campaign.peekEvents()).hasSize(1);
        assertThat(campaign.peekEvents().get(0).payload()).isInstanceOf(PromotionEvents.PromotionCampaignCreated.class);
    }

    @Test
    void transitionStates() {
        Money budget = Money.of(new BigDecimal("10000000"), CCY);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(5, ChronoUnit.DAYS);

        PromotionCampaign campaign = PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT, budget, start, end, USER);

        campaign.activate();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);

        campaign.end();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED);
    }

    @Test
    void consumeAndReleaseBudget() {
        Money budget = Money.of(new BigDecimal("100000"), CCY);
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(5, ChronoUnit.DAYS);

        PromotionCampaign campaign = PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT, budget, start, end, USER);

        campaign.consumeBudget(Money.of(new BigDecimal("30000"), CCY));
        assertThat(campaign.getBudgetRemaining().amount()).isEqualByComparingTo(new BigDecimal("70000"));

        campaign.releaseBudget(Money.of(new BigDecimal("10000"), CCY));
        assertThat(campaign.getBudgetRemaining().amount()).isEqualByComparingTo(new BigDecimal("80000"));

        assertThatThrownBy(() -> campaign.consumeBudget(Money.of(new BigDecimal("90000"), CCY)))
                .isInstanceOf(PromotionException.class)
                .extracting("errorCode")
                .isEqualTo(PromotionErrorCode.CAMPAIGN_OUT_OF_BUDGET.getCode());
    }
}
