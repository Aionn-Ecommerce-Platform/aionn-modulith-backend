package com.aionn.identity.infrastructure.persistence.adapter.account;

import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.port.out.user.DataExportPort;
import com.aionn.identity.domain.valueobject.DataExportStatus;
import com.aionn.identity.infrastructure.persistence.entity.DataExportRequestEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.account.DataExportRequestRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataExportPersistenceAdapter implements DataExportPort {

    private final DataExportRequestRepository dataExportRequestRepository;
    private final UserRepository userRepository;

    @Override
    public DataExportRequestView save(String userId) {
        UserEntity user = userRepository.getReferenceById(userId);
        DataExportRequestEntity request = DataExportRequestEntity.builder()
                .exportRequestId(IdGenerator.ulid())
                .user(user)
                .status(DataExportStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
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
