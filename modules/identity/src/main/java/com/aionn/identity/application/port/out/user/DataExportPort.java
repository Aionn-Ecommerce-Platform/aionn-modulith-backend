package com.aionn.identity.application.port.out.user;

import com.aionn.identity.application.dto.user.view.DataExportRequestView;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DataExportPort {

    DataExportRequestView save(String userId);

    boolean hasActiveRequest(String userId);

    Optional<DataExportPayload> findPayload(String requestId, String userId);

    /**
     * Atomically moves queued requests to {@code PROCESSING} and returns them.
     *
     * <p>Rows are locked with {@code SKIP LOCKED}, so several application instances can run the worker
     * without building the same export twice.
     */
    List<PendingExport> claimPending(int limit);

    /** Marks a claimed request completed and stores the generated JSON document. */
    void complete(String requestId, String payload);

    /** Marks a claimed request failed so it stops blocking new exports for the user. */
    void fail(String requestId);

    /** Builds the export document for a user; runs on the worker thread, never on the request thread. */
    String buildPayload(String userId);

    record DataExportPayload(String requestId, String contentType, String payload, Instant completedAt) {
    }

    record PendingExport(String requestId, String userId) {
    }
}
