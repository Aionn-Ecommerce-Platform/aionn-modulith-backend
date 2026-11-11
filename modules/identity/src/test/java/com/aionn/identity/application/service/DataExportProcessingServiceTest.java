package com.aionn.identity.application.service;

import com.aionn.identity.application.port.out.user.DataExportPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportProcessingServiceTest {

    @Mock
    private DataExportPort dataExportPort;

    @InjectMocks
    private DataExportProcessingService service;

    @Test
    void completesEachClaimedExport() {
        when(dataExportPort.claimPending(10)).thenReturn(List.of(
                new DataExportPort.PendingExport("exp-1", "user-1"),
                new DataExportPort.PendingExport("exp-2", "user-2")));
        when(dataExportPort.buildPayload("user-1")).thenReturn("{\"user\":1}");
        when(dataExportPort.buildPayload("user-2")).thenReturn("{\"user\":2}");

        assertThat(service.processPending(10)).isEqualTo(2);

        verify(dataExportPort).complete("exp-1", "{\"user\":1}");
        verify(dataExportPort).complete("exp-2", "{\"user\":2}");
    }

    @Test
    void aFailedExportDoesNotStopTheRestOfTheBatch() {
        when(dataExportPort.claimPending(10)).thenReturn(List.of(
                new DataExportPort.PendingExport("exp-1", "user-1"),
                new DataExportPort.PendingExport("exp-2", "user-2")));
        when(dataExportPort.buildPayload("user-1")).thenThrow(new IllegalStateException("boom"));
        when(dataExportPort.buildPayload("user-2")).thenReturn("{\"user\":2}");

        assertThat(service.processPending(10)).isEqualTo(1);

        verify(dataExportPort).fail("exp-1");
        verify(dataExportPort, never()).complete("exp-1", "{}");
        verify(dataExportPort).complete("exp-2", "{\"user\":2}");
    }

    @Test
    void idlePollDoesNothing() {
        when(dataExportPort.claimPending(10)).thenReturn(List.of());

        assertThat(service.processPending(10)).isZero();

        verify(dataExportPort, never()).complete(any(), any());
    }
}
