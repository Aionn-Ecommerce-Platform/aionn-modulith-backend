package com.aionn.identity.infrastructure.persistence.adapter.account;

import com.aionn.identity.application.dto.user.view.DeletionRequestView;
import com.aionn.identity.application.port.out.user.AccountDeletionPort;
import com.aionn.identity.domain.valueobject.AccountDeletionStatus;
import com.aionn.identity.infrastructure.persistence.entity.AccountDeletionRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.AccountDeletionRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountDeletionPersistenceAdapter implements AccountDeletionPort {

    private final AccountDeletionRequestRepository accountDeletionRequestRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    public DeletionRequestView save(String userId, Instant scheduledDeletionAt) {
        UserEntity user = userRepository.getReferenceById(userId);
        AccountDeletionRequestEntity request = AccountDeletionRequestEntity.builder()
                .deletionRequestId(IdGenerator.ulid())
                .user(user)
                .status(AccountDeletionStatus.PENDING)
                .requestedAt(Instant.now(clock))
                .scheduledDeletionAt(scheduledDeletionAt)
                .build();
        AccountDeletionRequestEntity saved = accountDeletionRequestRepository.save(request);
        return toView(saved);
    }

    @Override
    public Optional<DeletionRequestView> findPendingByUserId(String userId) {
        return accountDeletionRequestRepository.findByUser_UserIdAndStatus(userId, AccountDeletionStatus.PENDING)
                .map(this::toView);
    }

    @Override
    public void cancel(String userId) {
        accountDeletionRequestRepository.findByUser_UserIdAndStatus(userId, AccountDeletionStatus.PENDING)
                .ifPresent(request -> {
                    request.setStatus(AccountDeletionStatus.CANCELLED);
                    request.setCanceledAt(Instant.now(clock));
                    accountDeletionRequestRepository.save(request);
                });
    }

    private DeletionRequestView toView(AccountDeletionRequestEntity entity) {
        return new DeletionRequestView(
                entity.getDeletionRequestId(),
                entity.getStatus().name(),
                entity.getRequestedAt(),
                entity.getScheduledDeletionAt());
    }
}
