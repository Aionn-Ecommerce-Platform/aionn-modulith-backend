package com.aionn.identity.application.service;

import com.aionn.identity.application.port.out.user.DataExportPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Materialises queued personal-data exports.
 *
 * <p>Claiming and building are separate statements on purpose: the claim commits as {@code PROCESSING}
 * before the (potentially slow) snapshot starts, so a crash leaves a row the stale-request sweep can
 * recover instead of a lock nobody owns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportProcessingService {

    private final DataExportPort dataExportPort;

    public int processPending(int batchSize) {
        int completed = 0;
        for (DataExportPort.PendingExport pending : dataExportPort.claimPending(batchSize)) {
            try {
                dataExportPort.complete(pending.requestId(), dataExportPort.buildPayload(pending.userId()));
                completed++;
            } catch (RuntimeException ex) {
                log.warn("Data export {} failed for user {}: {}",
                        pending.requestId(), pending.userId(), ex.getMessage());
                dataExportPort.fail(pending.requestId());
            }
        }
        return completed;
    }
}
