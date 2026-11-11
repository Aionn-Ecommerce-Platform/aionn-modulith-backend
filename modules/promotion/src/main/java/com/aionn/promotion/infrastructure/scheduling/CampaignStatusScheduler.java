package com.aionn.promotion.infrastructure.scheduling;

import com.aionn.promotion.application.service.PromotionCampaignService;
import com.aionn.promotion.infrastructure.config.properties.PromotionSchedulerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "promotion.scheduler", name = "enabled", havingValue = "true")
public class CampaignStatusScheduler {

    private final PromotionCampaignService campaignService;
    private final PromotionSchedulerProperties schedulerProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${promotion.scheduler.delay-ms:30000}")
    @SchedulerLock(name = "promotion-campaign-status", lockAtMostFor = "PT10M", lockAtLeastFor = "PT25S")
    public void run() {
        try {
            int changed = campaignService.processScheduledTransitions(
                    clock.instant(), schedulerProperties.batchSize());
            if (changed > 0) {
                log.info("Promotion campaign status sweep transitioned {} campaign(s)", changed);
            }
        } catch (Exception ex) {
            log.error("Campaign status sweep failed", ex);
        }
    }
}
