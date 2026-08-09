package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.DataExportStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataExportRequestTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void createRequestedCreatesPendingExport() {
        DataExportRequest request = DataExportRequest.createRequested("export-1", "user-1", CLOCK);

        assertThat(request.getRequestId()).isEqualTo("export-1");
        assertThat(request.getUserId()).isEqualTo("user-1");
        assertThat(request.getStatus()).isEqualTo(DataExportStatus.REQUESTED);
        assertThat(request.getRequestedAt()).isEqualTo(NOW);
        assertThat(request.getFileUrl()).isNull();
        assertThat(request.getCompletedAt()).isNull();
    }

    @Test
    void createRequestedRejectsBlankIdentifiers() {
        assertThatThrownBy(() -> DataExportRequest.createRequested(" ", "user-1", CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestId");
        assertThatThrownBy(() -> DataExportRequest.createRequested("export-1", " ", CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void completeRequiresProcessingExportAndFileUrl() {
        DataExportRequest request = DataExportRequest.createRequested("export-1", "user-1", CLOCK);

        assertThatThrownBy(() -> request.complete("https://cdn.aionn.test/export.zip", CLOCK))
                .isInstanceOf(IllegalStateException.class);

        request.markProcessing();

        assertThatThrownBy(() -> request.complete(" ", CLOCK))
                .isInstanceOf(IllegalArgumentException.class);

        request.complete("https://cdn.aionn.test/export.zip", CLOCK);

        assertThat(request.getStatus()).isEqualTo(DataExportStatus.COMPLETED);
        assertThat(request.getFileUrl()).isEqualTo("https://cdn.aionn.test/export.zip");
        assertThat(request.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void failClosesRequestedOrProcessingExport() {
        DataExportRequest requested = DataExportRequest.createRequested("export-1", "user-1", CLOCK);
        requested.fail(CLOCK);

        assertThat(requested.getStatus()).isEqualTo(DataExportStatus.FAILED);
        assertThat(requested.getCompletedAt()).isNotNull();

        DataExportRequest processing = DataExportRequest.createRequested("export-2", "user-1", CLOCK);
        processing.markProcessing();
        processing.fail(CLOCK);

        assertThat(processing.getStatus()).isEqualTo(DataExportStatus.FAILED);
        assertThat(processing.getCompletedAt()).isNotNull();
    }
}
