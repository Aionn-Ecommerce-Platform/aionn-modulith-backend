package com.aionn.identity.infrastructure.persistence.adapter.auth;

import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.AuthSessionDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.auth.AuthSessionRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthSessionPersistenceAdapter implements AuthSessionPersistencePort {

    private final AuthSessionRepository authSessionRepository;
    private final UserRepository userRepository;
    private final AuthSessionDomainMapper authSessionDomainMapper;

    @Override
    public AuthSession save(AuthSession session) {
        var existing = authSessionRepository.findById(session.getSessionId());
        if (existing.isPresent()) {
            var managed = existing.get();
            authSessionDomainMapper.updateEntity(managed, session);
            var savedEntity = authSessionRepository.save(managed);
            return authSessionDomainMapper.toDomain(savedEntity);
        }
        UserEntity userEntity = userRepository.getReferenceById(session.getUserId());
        var entity = authSessionDomainMapper.toEntity(session, userEntity);
        var savedEntity = authSessionRepository.save(entity);
        return authSessionDomainMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AuthSession> findById(String sessionId) {
        return authSessionRepository.findById(sessionId)
                .map(authSessionDomainMapper::toDomain);
    }

    @Override
    public List<AuthSession> findByUserId(String userId) {
        return authSessionRepository.findByUser_UserIdOrderByCreatedAtDesc(userId).stream()
                .map(authSessionDomainMapper::toDomain)
                .toList();
    }

    @Override
    public List<AuthSession> saveAll(List<AuthSession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        List<String> ids = sessions.stream().map(AuthSession::getSessionId).toList();
        Map<String, com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity> managed =
                authSessionRepository.findAllById(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity::getSessionId,
                                e -> e));
        Map<String, UserEntity> userRefs = new HashMap<>();
        var entities = sessions.stream()
                .map(session -> {
                    var existing = managed.get(session.getSessionId());
                    if (existing != null) {
                        authSessionDomainMapper.updateEntity(existing, session);
                        return existing;
                    }
                    return authSessionDomainMapper.toEntity(
                            session,
                            userRefs.computeIfAbsent(session.getUserId(), userRepository::getReferenceById));
                })
                .toList();
        var savedEntities = authSessionRepository.saveAll(entities);
        return savedEntities.stream()
                .map(authSessionDomainMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public int deleteIdleBefore(LocalDateTime cutoff) {
        return authSessionRepository.deleteIdleBefore(cutoff);
    }
}
