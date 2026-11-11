package com.aionn.identity.infrastructure.persistence.adapter.account;

import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.port.out.user.DataExportPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.DataExportStatus;
import com.aionn.identity.infrastructure.persistence.entity.DataExportRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.DataExportRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String EXPORT_ID = "01HZEXP0000000000000000001";
    private static final Instant FIXED_NOW = Instant.parse("2026-07-12T10:00:00Z");

    @Mock
    private DataExportRequestRepository dataExportRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private DataExportPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DataExportPersistenceAdapter(dataExportRequestRepository, userRepository,
                jdbcTemplate, new ObjectMapper(), Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void saveQueuesTheRequestWithoutBuildingTheSnapshot() {
        when(userRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(UserEntity.builder().userId(USER_ID).build()));
        when(dataExportRequestRepository.existsByUser_UserIdAndStatusIn(eq(USER_ID), anyList()))
                .thenReturn(false);
        when(dataExportRequestRepository.save(any(DataExportRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DataExportRequestView view = adapter.save(USER_ID);

        assertThat(view.requestId()).isNotBlank();
        assertThat(view.status()).isEqualTo("REQUESTED");
        assertThat(view.requestedAt()).isEqualTo(FIXED_NOW);
        // The payload is built by the export worker, not on the request thread.
        verify(jdbcTemplate, never()).queryForList(anyString(), eq(USER_ID));
        verify(dataExportRequestRepository).failStaleRequests(
                eq(USER_ID),
                eq(DataExportStatus.FAILED),
                anyList(),
                eq(FIXED_NOW.minus(Duration.ofHours(1))),
                eq(FIXED_NOW));
        verify(dataExportRequestRepository).save(any(DataExportRequestEntity.class));
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
        when(userRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(UserEntity.builder().userId(USER_ID).build()));
        when(dataExportRequestRepository.existsByUser_UserIdAndStatusIn(eq(USER_ID), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> adapter.save(USER_ID))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(IdentityErrorCode.DATA_EXPORT_ALREADY_IN_PROGRESS.getCode()));
        verify(dataExportRequestRepository, never()).save(any());
    }

    @Test
    void findPayloadReturnsOnlyTheOwnersCompletedExport() {
        UserEntity owner = UserEntity.builder().userId(USER_ID).build();
        DataExportRequestEntity entity = DataExportRequestEntity.builder()
                .exportRequestId(EXPORT_ID)
                .user(owner)
                .status(DataExportStatus.COMPLETED)
                .requestedAt(FIXED_NOW)
                .completedAt(FIXED_NOW)
                .fileUrl("{\"profile\":{}}")
                .build();
        when(dataExportRequestRepository.findById(EXPORT_ID)).thenReturn(Optional.of(entity));

        Optional<DataExportPort.DataExportPayload> payload = adapter.findPayload(EXPORT_ID, USER_ID);

        assertThat(payload).isPresent();
        assertThat(payload.get().contentType()).isEqualTo("application/json");
        assertThat(payload.get().payload()).isEqualTo("{\"profile\":{}}");
    }

    @Test
    void findPayloadRejectsAnotherUsersExport() {
        UserEntity owner = UserEntity.builder().userId(USER_ID).build();
        DataExportRequestEntity entity = DataExportRequestEntity.builder()
                .exportRequestId(EXPORT_ID)
                .user(owner)
                .status(DataExportStatus.COMPLETED)
                .requestedAt(FIXED_NOW)
                .completedAt(FIXED_NOW)
                .fileUrl("{}")
                .build();
        when(dataExportRequestRepository.findById(EXPORT_ID)).thenReturn(Optional.of(entity));

        assertThat(adapter.findPayload(EXPORT_ID, "someone-else")).isEmpty();
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
