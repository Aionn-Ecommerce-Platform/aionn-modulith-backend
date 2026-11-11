package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.port.out.kyc.KycDocumentStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs daily at 03:41 server time (override with {@code identity.kyc.legacy-cleanup-cron}).
 * Cleans KYC assets and rows left behind by accounts tombstoned before the current erasure
 * flow existed. Kept separate from {@link AccountDeletionScheduler} because those deletion
 * requests are already COMPLETED and no longer appear in its pending queue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyKycCleanupScheduler {

    private static final int BATCH_SIZE = 100;
    private static final String DELETED_USERS = "SELECT user_id FROM users WHERE status = 'DELETED'";

    private final JdbcTemplate jdbcTemplate;
    private final KycDocumentStoragePort kycDocumentStoragePort;

    @Scheduled(cron = "${identity.kyc.legacy-cleanup-cron:0 41 3 * * *}")
    @SchedulerLock(name = "identity-legacy-kyc-cleanup", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    @Transactional
    public void cleanup() {
        List<String> publicIds = jdbcTemplate.queryForList(
                "SELECT d.public_id FROM kyc_documents d "
                        + "JOIN kyc_profiles p ON p.kyc_id = d.kyc_id "
                        + "WHERE d.public_id IS NOT NULL "
                        + "AND p.user_id IN (" + DELETED_USERS + ") "
                        + "LIMIT ?",
                String.class, BATCH_SIZE);
        for (String publicId : publicIds) {
            kycDocumentStoragePort.delete(publicId);
        }

        // Rows are removed only after all blobs in this batch were revoked successfully.
        jdbcTemplate.update("DELETE FROM kyc_documents WHERE kyc_id IN "
                + "(SELECT kyc_id FROM kyc_profiles WHERE user_id IN (" + DELETED_USERS + "))");
        jdbcTemplate.update("DELETE FROM kyc_profiles WHERE user_id IN (" + DELETED_USERS + ")");
        if (!publicIds.isEmpty()) {
            log.info("Cleaned up {} legacy KYC assets for deleted accounts", publicIds.size());
        }
    }
}
