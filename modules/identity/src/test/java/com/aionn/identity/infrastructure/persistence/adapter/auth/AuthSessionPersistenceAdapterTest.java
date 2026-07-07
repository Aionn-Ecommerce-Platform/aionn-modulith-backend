package com.aionn.identity.infrastructure.persistence.adapter.auth;

import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.infrastructure.persistence.entity.AuthSessionEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.AuthSessionDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.auth.AuthSessionRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionPersistenceAdapterTest {

    private static final String SESSION_ID = "01HZSES0000000000000000001";
    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private AuthSessionRepository authSessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthSessionDomainMapper authSessionDomainMapper;

    @InjectMocks
    private AuthSessionPersistenceAdapter adapter;

    private AuthSession session(String sessionId) {
        return AuthSession.createNew(sessionId, USER_ID, "127.0.0.1", "agent", LocalDateTime.now().plusHours(1));
    }

    @Test
    void saveUpdatesManagedEntityWhenAlreadyPersisted() {
        AuthSession session = session(SESSION_ID);
        AuthSessionEntity managed = mock(AuthSessionEntity.class);
        when(authSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(managed));
        when(authSessionRepository.save(managed)).thenReturn(managed);
        when(authSessionDomainMapper.toDomain(managed)).thenReturn(session);

        assertThat(adapter.save(session)).isSameAs(session);
        verify(authSessionDomainMapper).updateEntity(managed, session);
        verify(userRepository, never()).getReferenceById(any());
    }

    @Test
    void saveInsertsNewEntityWhenNotPersisted() {
        AuthSession session = session(SESSION_ID);
        AuthSessionEntity entity = mock(AuthSessionEntity.class);
        UserEntity user = UserEntity.builder().userId(USER_ID).build();
        when(authSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(authSessionDomainMapper.toEntity(session, user)).thenReturn(entity);
        when(authSessionRepository.save(entity)).thenReturn(entity);
        when(authSessionDomainMapper.toDomain(entity)).thenReturn(session);

        assertThat(adapter.save(session)).isSameAs(session);
    }

    @Test
    void findByIdReturnsMappedWhenPresent() {
        AuthSessionEntity entity = mock(AuthSessionEntity.class);
        AuthSession session = session(SESSION_ID);
        when(authSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(entity));
        when(authSessionDomainMapper.toDomain(entity)).thenReturn(session);

        assertThat(adapter.findById(SESSION_ID)).contains(session);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(authSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(SESSION_ID)).isEmpty();
    }

    @Test
    void findByUserIdMapsResults() {
        AuthSessionEntity entity = mock(AuthSessionEntity.class);
        AuthSession session = session(SESSION_ID);
        when(authSessionRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(entity));
        when(authSessionDomainMapper.toDomain(entity)).thenReturn(session);

        assertThat(adapter.findByUserId(USER_ID)).containsExactly(session);
    }

    @Test
    void saveAllReturnsEmptyWithoutRepositoryCallWhenInputEmpty() {
        assertThat(adapter.saveAll(List.of())).isEmpty();
        verify(authSessionRepository, never()).findAllById(anyList());
    }

    @Test
    void saveAllInsertsNewSessions() {
        AuthSession session = session(SESSION_ID);
        AuthSessionEntity entity = mock(AuthSessionEntity.class);
        UserEntity user = UserEntity.builder().userId(USER_ID).build();
        when(authSessionRepository.findAllById(List.of(SESSION_ID))).thenReturn(List.of());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(authSessionDomainMapper.toEntity(session, user)).thenReturn(entity);
        when(authSessionRepository.saveAll(List.of(entity))).thenReturn(List.of(entity));
        when(authSessionDomainMapper.toDomain(entity)).thenReturn(session);

        assertThat(adapter.saveAll(List.of(session))).containsExactly(session);
    }

    @Test
    void saveAllUpdatesExistingSessions() {
        AuthSession session = session(SESSION_ID);
        AuthSessionEntity managed = mock(AuthSessionEntity.class);
        when(managed.getSessionId()).thenReturn(SESSION_ID);
        when(authSessionRepository.findAllById(List.of(SESSION_ID))).thenReturn(List.of(managed));
        when(authSessionRepository.saveAll(List.of(managed))).thenReturn(List.of(managed));
        when(authSessionDomainMapper.toDomain(managed)).thenReturn(session);

        assertThat(adapter.saveAll(List.of(session))).containsExactly(session);
        verify(authSessionDomainMapper).updateEntity(managed, session);
        verify(userRepository, never()).getReferenceById(any());
    }

    @Test
    void deleteIdleBeforeDelegatesToRepository() {
        LocalDateTime cutoff = LocalDateTime.now();
        when(authSessionRepository.deleteIdleBefore(cutoff)).thenReturn(7);

        assertThat(adapter.deleteIdleBefore(cutoff)).isEqualTo(7);
    }
}
