package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.service.DataExportProcessingService;
import com.aionn.identity.infrastructure.config.properties.DataExportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Builds requested data exports off the request thread.
 *
 * <p>The user-facing POST only records the request; this worker turns it into a document. Claims use
 * {@code FOR UPDATE SKIP LOCKED}, so the ShedLock lease is a cost guard rather than the only safety net.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataExportScheduler {

    private final DataExportProcessingService processingService;
    private final DataExportProperties properties;

    @Scheduled(fixedDelayString = "${identity.data-export.delay-ms:5000}")
    @SchedulerLock(name = "data-export-processing", lockAtMostFor = "PT5M", lockAtLeastFor = "PT2S")
    public void processRequestedExports() {
        try {
            int completed = processingService.processPending(properties.batchSize());
            if (completed > 0) {
                log.info("Completed {} data export request(s)", completed);
            }
        } catch (RuntimeException ex) {
            log.error("Data export sweep failed", ex);
        }
    }
}
