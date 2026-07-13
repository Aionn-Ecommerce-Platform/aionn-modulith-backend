package com.aionn.inventory.infrastructure.scheduling;

import com.aionn.inventory.application.port.out.StockReservationPersistencePort;
import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.infrastructure.config.InventoryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory.reservation.auto-release", name = "enabled", havingValue = "true")
public class ReservationAutoReleaseScheduler {

    private final StockReservationPersistencePort reservationRepository;
    private final ReservationAutoReleaseWorker worker;
    private final InventoryProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${inventory.reservation.auto-release.delay-ms:30000}")
    public void releaseExpired() {
        try {
            int batchSize = properties.reservation().autoRelease().batchSize();
            List<StockReservation> expired = reservationRepository.findExpired(clock.instant(), batchSize);
            int released = 0;
            for (StockReservation reservation : expired) {
                try {
                    worker.releaseOne(reservation.getReservationId());
                    released++;
                } catch (RuntimeException ex) {
                    log.warn("Skip expired reservation {}: {}", reservation.getReservationId(), ex.getMessage());
                }
            }
            if (released > 0) {
                log.info("Auto-released {} expired reservation(s)", released);
            }
        } catch (Exception ex) {
            log.error("Failed to auto-release expired reservations", ex);
        }
    }
}
