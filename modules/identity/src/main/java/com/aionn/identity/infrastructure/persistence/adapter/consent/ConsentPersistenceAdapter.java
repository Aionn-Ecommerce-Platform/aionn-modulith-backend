package com.aionn.identity.infrastructure.persistence.adapter.consent;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.application.port.out.consent.ConsentPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.UserConsent;
import com.aionn.identity.infrastructure.persistence.entity.UserConsentEntity;
import com.aionn.identity.infrastructure.persistence.mapper.ConsentResultMapper;
import com.aionn.identity.infrastructure.persistence.repository.consent.UserConsentRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConsentPersistenceAdapter implements ConsentPersistencePort {

    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final ConsentResultMapper consentResultMapper;

    @Override
    public ConsentResult append(UserConsent consent) {
        var user = userRepository.findById(consent.getUserId())
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        UserConsentEntity entity = UserConsentEntity.builder()
                .consentId(consent.getId())
                .user(user)
                .consentType(consent.getConsentType().name())
                .version(consent.getVersion())
                .ipAddress(consent.getIpAddress())
                .revokedAt(consent.isGranted() ? null : consent.getRevokedAt())
                .build();
        UserConsentEntity saved = consentRepository.save(entity);
        return consentResultMapper.toResult(saved);
    }

    @Override
    public Optional<ConsentResult> findLatest(String userId, String consentType) {
        return consentRepository
                .findTopByUser_UserIdAndConsentTypeOrderByAgreedAtDesc(userId, consentType)
                .map(consentResultMapper::toResult);
    }

    @Override
    public List<ConsentResult> findHistory(String userId) {
        return consentResultMapper.toResults(
                consentRepository.findByUser_UserIdOrderByAgreedAtDesc(userId));
    }
}
