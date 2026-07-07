package com.aionn.identity.infrastructure.persistence.adapter.agent;

import com.aionn.identity.domain.model.AgentIdentity;
import com.aionn.identity.domain.valueobject.AgentStatus;
import com.aionn.identity.infrastructure.persistence.entity.AgentIdentityEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.AgentIdentityDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.agent.AgentIdentityRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPersistenceAdapterTest {

    private static final String AGENT_ID = "01HZAGT0000000000000000001";
    private static final String OWNER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private AgentIdentityRepository agentIdentityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AgentIdentityDomainMapper mapper;

    @InjectMocks
    private AgentPersistenceAdapter adapter;

    private AgentIdentity agent() {
        return AgentIdentity.builder()
                .id(AGENT_ID)
                .ownerId(OWNER_ID)
                .name("agent")
                .keyHash("hash")
                .permissions("read")
                .status(AgentStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void saveAttachesOwnerAndMapsBack() {
        AgentIdentity agent = agent();
        AgentIdentityEntity entity = mock(AgentIdentityEntity.class);
        UserEntity owner = UserEntity.builder().userId(OWNER_ID).build();
        when(mapper.toEntity(agent)).thenReturn(entity);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(agentIdentityRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(agent);

        assertThat(adapter.save(agent)).isSameAs(agent);
        verify(entity).setOwner(owner);
    }

    @Test
    void findByIdReturnsMappedWhenPresent() {
        AgentIdentityEntity entity = mock(AgentIdentityEntity.class);
        AgentIdentity agent = agent();
        when(agentIdentityRepository.findById(AGENT_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(agent);

        assertThat(adapter.findById(AGENT_ID)).contains(agent);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(agentIdentityRepository.findById(AGENT_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(AGENT_ID)).isEmpty();
    }

    @Test
    void findByOwnerIdMapsResults() {
        AgentIdentityEntity entity = mock(AgentIdentityEntity.class);
        AgentIdentity agent = agent();
        when(agentIdentityRepository.findByOwner_UserIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(agent);

        assertThat(adapter.findByOwnerId(OWNER_ID)).containsExactly(agent);
    }

    @Test
    void findByKeyHashReturnsMappedWhenPresent() {
        AgentIdentityEntity entity = mock(AgentIdentityEntity.class);
        AgentIdentity agent = agent();
        when(agentIdentityRepository.findByKeyHash("hash")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(agent);

        assertThat(adapter.findByKeyHash("hash")).contains(agent);
    }

    @Test
    void findByKeyHashReturnsEmptyWhenMissing() {
        when(agentIdentityRepository.findByKeyHash("hash")).thenReturn(Optional.empty());

        assertThat(adapter.findByKeyHash("hash")).isEmpty();
    }

    @Test
    void findByIdAndOwnerIdReturnsMappedWhenPresent() {
        AgentIdentityEntity entity = mock(AgentIdentityEntity.class);
        AgentIdentity agent = agent();
        when(agentIdentityRepository.findByAgentIdAndOwner_UserId(AGENT_ID, OWNER_ID))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(agent);

        assertThat(adapter.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).contains(agent);
    }

    @Test
    void findByIdAndOwnerIdReturnsEmptyWhenMissing() {
        when(agentIdentityRepository.findByAgentIdAndOwner_UserId(AGENT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        assertThat(adapter.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).isEmpty();
    }

    @Test
    void updateAttachesOwnerAndMapsBack() {
        AgentIdentity agent = agent();
        AgentIdentityEntity entity = mock(AgentIdentityEntity.class);
        UserEntity owner = UserEntity.builder().userId(OWNER_ID).build();
        when(mapper.toEntity(agent)).thenReturn(entity);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(agentIdentityRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(agent);

        assertThat(adapter.update(agent)).isSameAs(agent);
        verify(entity).setOwner(owner);
    }

    @Test
    void deleteByIdAndOwnerIdDeletesWhenPresent() {
        AgentIdentityEntity entity = mock(AgentIdentityEntity.class);
        when(agentIdentityRepository.findByAgentIdAndOwner_UserId(AGENT_ID, OWNER_ID))
                .thenReturn(Optional.of(entity));

        adapter.deleteByIdAndOwnerId(AGENT_ID, OWNER_ID);

        verify(agentIdentityRepository).delete(entity);
    }

    @Test
    void deleteByIdAndOwnerIdDoesNothingWhenMissing() {
        when(agentIdentityRepository.findByAgentIdAndOwner_UserId(AGENT_ID, OWNER_ID))
                .thenReturn(Optional.empty());

        adapter.deleteByIdAndOwnerId(AGENT_ID, OWNER_ID);

        verify(agentIdentityRepository, never()).delete(any(AgentIdentityEntity.class));
    }
}
