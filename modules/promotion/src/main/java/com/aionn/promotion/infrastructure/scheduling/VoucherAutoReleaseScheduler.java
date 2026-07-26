package com.aionn.promotion.infrastructure.scheduling;

import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.domain.model.UserVoucher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "promotion.voucher.auto-release", name = "enabled", havingValue = "true")
public class VoucherAutoReleaseScheduler {

    private final UserVoucherPersistencePort userVoucherRepository;
    private final VoucherAutoReleaseWorker worker;

    @Value("${promotion.voucher.auto-release.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${promotion.voucher.auto-release.delay-ms:30000}")
    public void run() {
        try {
            List<UserVoucher> expired = userVoucherRepository.findExpiredReservations(Instant.now(), batchSize);
            int released = 0;
            for (UserVoucher uv : expired) {
                try {
                    if (worker.releaseOne(uv.getUserVoucherId())) {
                        released++;
                    }
                } catch (RuntimeException ex) {
                    log.warn("Skip release for {}: {}", uv.getUserVoucherId(), ex.getMessage());
                }
            }
            if (released > 0) {
                log.info("Voucher auto-release returned {} expired reservation(s) to the pool", released);
            }
        } catch (Exception ex) {
            log.error("Voucher auto-release sweep failed", ex);
        }
    }
}
