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
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataExportPersistenceAdapter implements DataExportPort {

    private final DataExportRequestRepository dataExportRequestRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    @Transactional
    public DataExportRequestView save(String userId) {
        // Serialize concurrent requests for the same user via the user row lock
        // so two REQUESTED/PROCESSING rows can never be inserted at once.
        UserEntity user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
        boolean active = dataExportRequestRepository.existsByUser_UserIdAndStatusIn(
                userId,
                List.of(DataExportStatus.REQUESTED, DataExportStatus.PROCESSING));
        if (active) {
            throw new IdentityException(IdentityErrorCode.DATA_EXPORT_ALREADY_IN_PROGRESS);
        }
        DataExportRequestEntity request = DataExportRequestEntity.builder()
                .exportRequestId(IdGenerator.ulid())
                .user(user)
                .status(DataExportStatus.REQUESTED)
                .requestedAt(Instant.now(clock))
                .build();
        DataExportRequestEntity saved = dataExportRequestRepository.save(request);
        return toView(saved);
    }

    @Override
    public boolean hasActiveRequest(String userId) {
        return dataExportRequestRepository.existsByUser_UserIdAndStatusIn(
                userId,
                List.of(DataExportStatus.REQUESTED, DataExportStatus.PROCESSING));
    }

    private DataExportRequestView toView(DataExportRequestEntity entity) {
        return new DataExportRequestView(
                entity.getExportRequestId(),
                entity.getStatus().name(),
                entity.getRequestedAt());
    }
}
