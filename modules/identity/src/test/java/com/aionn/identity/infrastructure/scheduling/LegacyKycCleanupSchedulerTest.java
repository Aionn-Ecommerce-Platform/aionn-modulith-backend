package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.port.out.kyc.KycDocumentStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyKycCleanupSchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private KycDocumentStoragePort kycDocumentStoragePort;

    private LegacyKycCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new LegacyKycCleanupScheduler(jdbcTemplate, kycDocumentStoragePort);
    }

    @Test
    void deletesBlobsBeforeRowsForDeletedUsers() {
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq(100)))
                .thenReturn(List.of("kyc-public-1", "kyc-public-2"));

        scheduler.cleanup();

        verify(kycDocumentStoragePort).delete("kyc-public-1");
        verify(kycDocumentStoragePort).delete("kyc-public-2");
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("DELETE FROM kyc_documents"));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("DELETE FROM kyc_profiles"));
    }

    @Test
    void abortsBeforeDeletingRowsWhenBlobDeletionFails() {
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq(100)))
                .thenReturn(List.of("kyc-public-1"));
        doThrow(new IllegalStateException("cloudinary down"))
                .when(kycDocumentStoragePort).delete("kyc-public-1");

        org.assertj.core.api.Assertions.assertThatThrownBy(scheduler::cleanup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cloudinary down");

        verify(jdbcTemplate, never()).update(any(String.class));
    }
}
