package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.DataExportStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataExportRequestTest {

    @Test
    void createRequestedCreatesPendingExport() {
        DataExportRequest request = DataExportRequest.createRequested("export-1", "user-1");

        assertThat(request.getRequestId()).isEqualTo("export-1");
        assertThat(request.getUserId()).isEqualTo("user-1");
        assertThat(request.getStatus()).isEqualTo(DataExportStatus.REQUESTED);
        assertThat(request.getRequestedAt()).isNotNull();
        assertThat(request.getFileUrl()).isNull();
        assertThat(request.getCompletedAt()).isNull();
    }

    @Test
    void createRequestedRejectsBlankIdentifiers() {
        assertThatThrownBy(() -> DataExportRequest.createRequested(" ", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestId");
        assertThatThrownBy(() -> DataExportRequest.createRequested("export-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void completeRequiresProcessingExportAndFileUrl() {
        DataExportRequest request = DataExportRequest.createRequested("export-1", "user-1");

        assertThatThrownBy(() -> request.complete("https://cdn.aionn.test/export.zip"))
                .isInstanceOf(IllegalStateException.class);

        request.markProcessing();

        assertThatThrownBy(() -> request.complete(" "))
                .isInstanceOf(IllegalArgumentException.class);

        request.complete("https://cdn.aionn.test/export.zip");

        assertThat(request.getStatus()).isEqualTo(DataExportStatus.COMPLETED);
        assertThat(request.getFileUrl()).isEqualTo("https://cdn.aionn.test/export.zip");
        assertThat(request.getCompletedAt()).isNotNull();
    }

    @Test
    void failClosesRequestedOrProcessingExport() {
        DataExportRequest requested = DataExportRequest.createRequested("export-1", "user-1");
        requested.fail();

        assertThat(requested.getStatus()).isEqualTo(DataExportStatus.FAILED);
        assertThat(requested.getCompletedAt()).isNotNull();

        DataExportRequest processing = DataExportRequest.createRequested("export-2", "user-1");
        processing.markProcessing();
        processing.fail();

        assertThat(processing.getStatus()).isEqualTo(DataExportStatus.FAILED);
        assertThat(processing.getCompletedAt()).isNotNull();
    }
}
