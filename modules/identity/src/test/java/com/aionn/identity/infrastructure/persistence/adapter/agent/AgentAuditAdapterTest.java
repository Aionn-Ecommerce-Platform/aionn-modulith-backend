package com.aionn.identity.infrastructure.persistence.adapter.agent;

import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.identity.infrastructure.persistence.entity.SecurityAuditEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.SecurityAuditDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.security.SecurityAuditRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAuditAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String AGENT_ID = "01HZAGT0000000000000000001";

    @Mock
    private SecurityAuditRepository securityAuditRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityAuditDomainMapper mapper;

    @InjectMocks
    private AgentAuditAdapter adapter;

    private SecurityAudit audit() {
        return SecurityAudit.builder()
                .id("01HZAUD0000000000000000001")
                .userId(USER_ID)
                .eventType(SecurityAuditEventType.AGENT_SUSPENDED)
                .description("Agent suspended")
                .ipAddress("127.0.0.1")
                .deviceId(AGENT_ID)
                .build();
    }

    @Test
    void saveAttachesUserAndMapsBack() {
        SecurityAudit audit = audit();
        SecurityAuditEntity entity = SecurityAuditEntity.builder().auditId(audit.getId()).build();
        UserEntity user = UserEntity.builder().userId(USER_ID).build();
        when(mapper.toEntity(audit)).thenReturn(entity);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(securityAuditRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(audit);

        SecurityAudit result = adapter.save(audit);

        assertThat(result).isSameAs(audit);
        assertThat(entity.getUser()).isSameAs(user);
        verify(securityAuditRepository).save(entity);
    }

    @Test
    void findByAgentIdMapsResultsAndBuildsPageRequest() {
        SecurityAuditEntity entity = SecurityAuditEntity.builder().auditId("01HZAUD0000000000000000002").build();
        SecurityAudit domain = audit();
        when(securityAuditRepository.findByDeviceIdOrderByTimestampDesc(eq(AGENT_ID), any(PageRequest.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<SecurityAudit> result = adapter.findByAgentId(AGENT_ID, 25);

        assertThat(result).containsExactly(domain);
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(securityAuditRepository).findByDeviceIdOrderByTimestampDesc(eq(AGENT_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(25);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }
}
