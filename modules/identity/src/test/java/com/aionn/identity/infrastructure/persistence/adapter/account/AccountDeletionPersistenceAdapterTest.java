package com.aionn.identity.infrastructure.persistence.adapter.account;

import com.aionn.identity.application.dto.user.view.DeletionRequestView;
import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import com.aionn.identity.infrastructure.persistence.entity.AccountDeletionRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.AccountDeletionRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String REQUEST_ID = "01HZDEL0000000000000000001";

    @Mock
    private AccountDeletionRequestRepository accountDeletionRequestRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountDeletionPersistenceAdapter adapter;

    @Test
    void saveBuildsPendingRequestAndReturnsView() {
        Instant scheduledAt = Instant.now().plus(Duration.ofDays(30));
        Instant requestedAt = Instant.now();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(UserEntity.builder().build());
        when(accountDeletionRequestRepository.save(any(AccountDeletionRequestEntity.class)))
                .thenReturn(AccountDeletionRequestEntity.builder()
                        .deletionRequestId(REQUEST_ID)
                        .status(AccountDeletionStatus.PENDING)
                        .requestedAt(requestedAt)
                        .scheduledDeletionAt(scheduledAt)
                        .build());

        DeletionRequestView view = adapter.save(USER_ID, scheduledAt);

        assertThat(view.requestId()).isEqualTo(REQUEST_ID);
        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.requestedAt()).isEqualTo(requestedAt);
        assertThat(view.scheduledDeletionAt()).isEqualTo(scheduledAt);
    }

    @Test
    void findPendingByUserIdReturnsMappedViewWhenPresent() {
        AccountDeletionRequestEntity entity = AccountDeletionRequestEntity.builder()
                .deletionRequestId(REQUEST_ID)
                .status(AccountDeletionStatus.PENDING)
                .requestedAt(Instant.now())
                .scheduledDeletionAt(Instant.now().plus(Duration.ofDays(30)))
                .build();
        when(accountDeletionRequestRepository.findByUser_UserIdAndStatus(USER_ID, AccountDeletionStatus.PENDING))
                .thenReturn(Optional.of(entity));

        assertThat(adapter.findPendingByUserId(USER_ID))
                .hasValueSatisfying(view -> assertThat(view.requestId()).isEqualTo(REQUEST_ID));
    }

    @Test
    void findPendingByUserIdReturnsEmptyWhenMissing() {
        when(accountDeletionRequestRepository.findByUser_UserIdAndStatus(USER_ID, AccountDeletionStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThat(adapter.findPendingByUserId(USER_ID)).isEmpty();
    }

    @Test
    void cancelMarksPendingRequestCancelledAndSaves() {
        AccountDeletionRequestEntity entity = AccountDeletionRequestEntity.builder()
                .deletionRequestId(REQUEST_ID)
                .status(AccountDeletionStatus.PENDING)
                .requestedAt(Instant.now())
                .scheduledDeletionAt(Instant.now().plus(Duration.ofDays(30)))
                .build();
        when(accountDeletionRequestRepository.findByUser_UserIdAndStatus(USER_ID, AccountDeletionStatus.PENDING))
                .thenReturn(Optional.of(entity));

        adapter.cancel(USER_ID);

        assertThat(entity.getStatus()).isEqualTo(AccountDeletionStatus.CANCELLED);
        assertThat(entity.getCanceledAt()).isNotNull();
        verify(accountDeletionRequestRepository).save(entity);
    }

    @Test
    void cancelDoesNothingWhenNoPendingRequest() {
        when(accountDeletionRequestRepository.findByUser_UserIdAndStatus(USER_ID, AccountDeletionStatus.PENDING))
                .thenReturn(Optional.empty());

        adapter.cancel(USER_ID);

        verify(accountDeletionRequestRepository, never()).save(any());
    }
}
