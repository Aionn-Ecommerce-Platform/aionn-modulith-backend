package com.aionn.identity.infrastructure.persistence.adapter.account;

import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.DataExportStatus;
import com.aionn.identity.infrastructure.persistence.entity.DataExportRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.DataExportRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String EXPORT_ID = "01HZEXP0000000000000000001";

    @Mock
    private DataExportRequestRepository dataExportRequestRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DataExportPersistenceAdapter adapter;

    @Test
    void saveCreatesRequestedExportWhenNoneActive() {
        Instant requestedAt = Instant.now();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(dataExportRequestRepository.existsByUser_UserIdAndStatusIn(eq(USER_ID), anyList())).thenReturn(false);
        when(dataExportRequestRepository.save(any(DataExportRequestEntity.class)))
                .thenReturn(DataExportRequestEntity.builder()
                        .exportRequestId(EXPORT_ID)
                        .status(DataExportStatus.REQUESTED)
                        .requestedAt(requestedAt)
                        .build());

        DataExportRequestView view = adapter.save(USER_ID);

        assertThat(view.requestId()).isEqualTo(EXPORT_ID);
        assertThat(view.status()).isEqualTo("REQUESTED");
        assertThat(view.requestedAt()).isEqualTo(requestedAt);
    }

    @Test
    void saveThrowsWhenUserMissing() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(USER_ID))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(dataExportRequestRepository, never()).save(any());
    }

    @Test
    void saveThrowsWhenExportAlreadyInProgress() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(dataExportRequestRepository.existsByUser_UserIdAndStatusIn(eq(USER_ID), anyList())).thenReturn(true);

        assertThatThrownBy(() -> adapter.save(USER_ID))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(IdentityErrorCode.DATA_EXPORT_ALREADY_IN_PROGRESS.getCode()));
        verify(dataExportRequestRepository, never()).save(any());
    }

    @Test
    void hasActiveRequestDelegatesToRepository() {
        when(dataExportRequestRepository.existsByUser_UserIdAndStatusIn(eq(USER_ID), anyList())).thenReturn(true);

        assertThat(adapter.hasActiveRequest(USER_ID)).isTrue();
    }

    @Test
    void hasActiveRequestReturnsFalseWhenNoneActive() {
        when(dataExportRequestRepository.existsByUser_UserIdAndStatusIn(eq(USER_ID), anyList())).thenReturn(false);

        assertThat(adapter.hasActiveRequest(USER_ID)).isFalse();
    }
}
