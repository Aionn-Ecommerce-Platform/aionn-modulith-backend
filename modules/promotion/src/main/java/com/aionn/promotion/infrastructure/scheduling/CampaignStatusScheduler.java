package com.aionn.promotion.infrastructure.scheduling;

import com.aionn.promotion.application.service.PromotionCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "promotion.scheduler", name = "enabled", havingValue = "true")
public class CampaignStatusScheduler {

    private final PromotionCampaignService campaignService;

    @Value("${promotion.scheduler.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${promotion.scheduler.delay-ms:30000}")
    public void run() {
        try {
            int changed = campaignService.processScheduledTransitions(Instant.now(), batchSize);
            if (changed > 0) {
                log.info("Promotion campaign status sweep transitioned {} campaign(s)", changed);
            }
        } catch (Exception ex) {
            log.error("Campaign status sweep failed", ex);
        }
    }
}
