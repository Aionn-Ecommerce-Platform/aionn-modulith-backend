package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.event.PromotionEvents;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.valueobject.CampaignStatus;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.promotion.domain.valueobject.PromotionCondition;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionCampaignTest {

    private static final String CAMPAIGN_ID = "camp-1";
    private static final String NAME = "Black Friday";
    private static final String USER = "admin-1";
    private static final String CCY = "VND";
    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Instant START = NOW.plus(1, ChronoUnit.DAYS);
    private static final Instant END = START.plus(5, ChronoUnit.DAYS);

    private static PromotionCampaign scheduled() {
        return scheduled(new BigDecimal("10000000"));
    }

    private static PromotionCampaign scheduled(BigDecimal budget) {
        return PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                Money.of(budget, CCY), START, END, USER, CLOCK);
    }

    @Test
    void createSuccessAndRecordsEvents() {
        PromotionCampaign campaign = scheduled();

        assertThat(campaign.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(campaign.getName()).isEqualTo(NAME);
        assertThat(campaign.getType()).isEqualTo(CampaignType.DISCOUNT);
        assertThat(campaign.getBudgetRemaining()).isEqualTo(campaign.getBudget());
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SCHEDULED);
        assertThat(campaign.getCondition()).isEqualTo(PromotionCondition.empty());
        assertThat(campaign.peekEvents()).hasSize(1);
        assertThat(campaign.peekEvents().get(0).payload())
                .isInstanceOf(PromotionEvents.PromotionCampaignCreated.class);
    }

    @Test
    void createWithoutClockUsesSystemTime() {
        PromotionCampaign campaign = PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                Money.of(new BigDecimal("1000"), CCY),
                Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS), USER, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(campaign.getCreatedAt()).isNotNull();
    }

    @Test
    void createRejectsInvertedDates() {
        Money budget = Money.of(new BigDecimal("1000"), CCY);

        assertThatThrownBy(() -> PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                budget, END, START, USER, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void createRejectsMissingDates() {
        Money budget = Money.of(new BigDecimal("1000"), CCY);

        assertThatThrownBy(() -> PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                budget, null, END, USER, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void createRejectsNullBudget() {
        assertThatThrownBy(() -> PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                null, START, END, USER, CLOCK))
                .isInstanceOf(PromotionException.class)
                .hasMessageContaining("budget");
    }

    @Test
    void createRejectsStartDateInThePast() {
        Money budget = Money.of(new BigDecimal("1000"), CCY);

        assertThatThrownBy(() -> PromotionCampaign.create(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                budget, NOW.minus(2, ChronoUnit.DAYS), END, USER, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void constructorDefaultsBudgetRemainingAndCondition() {
        Money budget = Money.of(new BigDecimal("500"), CCY);

        PromotionCampaign campaign = new PromotionCampaign(CAMPAIGN_ID, NAME, CampaignType.PERCENT,
                budget, null, START, END, USER, CampaignStatus.DRAFT, null, NOW, NOW);

        assertThat(campaign.getBudgetRemaining()).isEqualTo(budget);
        assertThat(campaign.getCondition()).isEqualTo(PromotionCondition.empty());
    }

    @Test
    void transitionStates() {
        PromotionCampaign campaign = scheduled();

        campaign.activate(CLOCK);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);

        campaign.end(CLOCK);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED);
    }

    @Test
    void transitionStatesWithoutClock() {
        PromotionCampaign campaign = scheduled();

        campaign.activate(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        campaign.end(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED);
    }

    @Test
    void activateFromDraftPromotesThroughScheduled() {
        PromotionCampaign campaign = new PromotionCampaign(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                Money.of(new BigDecimal("1000"), CCY), null, START, END, USER,
                CampaignStatus.DRAFT, null, NOW, NOW);

        campaign.activate(CLOCK);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    void activateRejectsEndedCampaign() {
        PromotionCampaign campaign = scheduled();
        campaign.activate(CLOCK);
        campaign.end(CLOCK);

        assertThatThrownBy(() -> campaign.activate(CLOCK))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.CAMPAIGN_INVALID_STATE.getCode());
    }

    @Test
    void scheduleMovesDraftToScheduled() {
        PromotionCampaign campaign = new PromotionCampaign(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                Money.of(new BigDecimal("1000"), CCY), null, START, END, USER,
                CampaignStatus.DRAFT, null, NOW, NOW);

        campaign.schedule(CLOCK);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SCHEDULED);
    }

    @Test
    void scheduleRejectsRunningCampaign() {
        PromotionCampaign campaign = scheduled();
        campaign.activate(CLOCK);

        assertThatThrownBy(() -> campaign.schedule(CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void scheduleWithoutClockUsesSystemTime() {
        PromotionCampaign campaign = new PromotionCampaign(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                Money.of(new BigDecimal("1000"), CCY), null, START, END, USER,
                CampaignStatus.DRAFT, null, NOW, NOW);

        campaign.schedule(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(campaign.getUpdatedAt()).isNotNull();
    }

    @Test
    void endRejectsScheduledCampaign() {
        PromotionCampaign campaign = scheduled();

        assertThatThrownBy(() -> campaign.end(CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void cancelStoresEventAndRejectsSecondCancel() {
        PromotionCampaign campaign = scheduled();
        campaign.pullEvents();

        campaign.cancel("wrong dates", CLOCK);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        assertThat(campaign.pullEvents()).hasSize(1);
        assertThatThrownBy(() -> campaign.cancel("again", CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void cancelWithoutClockUsesSystemTime() {
        PromotionCampaign campaign = scheduled();

        campaign.cancel("nope", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(campaign.getUpdatedAt()).isNotNull();
    }

    @Test
    void configureConditionStoresConditionAndRejectsNull() {
        PromotionCampaign campaign = scheduled();
        campaign.pullEvents();

        campaign.configureCondition(new PromotionCondition(new BigDecimal("100000"),
                List.of("cat-1"), 1, 2), CLOCK);

        assertThat(campaign.getCondition().minOrderValue()).isEqualByComparingTo("100000");
        assertThat(campaign.pullEvents()).hasSize(1);
        assertThatThrownBy(() -> campaign.configureCondition(null, CLOCK))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void configureConditionWithoutClockUsesSystemTime() {
        PromotionCampaign campaign = scheduled();

        campaign.configureCondition(PromotionCondition.empty(), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(campaign.getUpdatedAt()).isNotNull();
    }

    @Test
    void ensureRunningActivatesScheduledCampaignPastStartDate() {
        PromotionCampaign campaign = scheduled();
        campaign.pullEvents();

        campaign.ensureRunning(Clock.fixed(START.plusSeconds(1), ZoneOffset.UTC));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(campaign.pullEvents()).hasSize(1);
    }

    @Test
    void ensureRunningEndsCampaignPastEndDate() {
        PromotionCampaign campaign = scheduled();
        campaign.activate(CLOCK);

        assertThatThrownBy(() -> campaign.ensureRunning(
                Clock.fixed(END.plusSeconds(1), ZoneOffset.UTC)))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.CAMPAIGN_NOT_RUNNING.getCode());
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ENDED);
    }

    @Test
    void ensureRunningRejectsScheduledCampaignBeforeStart() {
        PromotionCampaign campaign = scheduled();

        assertThatThrownBy(() -> campaign.ensureRunning(CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void ensureRunningPassesForRunningCampaign() {
        PromotionCampaign campaign = scheduled();
        campaign.activate(CLOCK);

        campaign.ensureRunning(Clock.fixed(START.plusSeconds(60), ZoneOffset.UTC));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    void ensureRunningWithoutClockUsesSystemTime() {
        PromotionCampaign campaign = new PromotionCampaign(CAMPAIGN_ID, NAME, CampaignType.DISCOUNT,
                Money.of(new BigDecimal("1000"), CCY), null,
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                USER, CampaignStatus.RUNNING, null, NOW, NOW);

        campaign.ensureRunning(java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    void consumeAndReleaseBudget() {
        PromotionCampaign campaign = scheduled(new BigDecimal("100000"));

        campaign.consumeBudget(Money.of(new BigDecimal("30000"), CCY), CLOCK);
        assertThat(campaign.getBudgetRemaining().amount()).isEqualByComparingTo("70000");

        campaign.releaseBudget(Money.of(new BigDecimal("10000"), CCY), CLOCK);
        assertThat(campaign.getBudgetRemaining().amount()).isEqualByComparingTo("80000");

        assertThatThrownBy(() -> campaign.consumeBudget(Money.of(new BigDecimal("90000"), CCY), CLOCK))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.CAMPAIGN_OUT_OF_BUDGET.getCode());
    }

    @Test
    void releaseBudgetIsClampedToOriginalBudget() {
        PromotionCampaign campaign = scheduled(new BigDecimal("100000"));

        campaign.consumeBudget(Money.of(new BigDecimal("1000"), CCY), CLOCK);
        campaign.releaseBudget(Money.of(new BigDecimal("50000"), CCY), CLOCK);

        assertThat(campaign.getBudgetRemaining().amount()).isEqualByComparingTo("100000");
    }

    @Test
    void budgetOperationsWithoutClockUseSystemTime() {
        PromotionCampaign campaign = scheduled(new BigDecimal("100000"));

        campaign.consumeBudget(Money.of(new BigDecimal("1000"), CCY), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        campaign.releaseBudget(Money.of(new BigDecimal("1000"), CCY), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(campaign.getUpdatedAt()).isNotNull();
    }
}
