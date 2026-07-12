package com.aionn.identity.infrastructure.persistence.adapter.consent;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.UserConsent;
import com.aionn.identity.domain.valueobject.ConsentType;
import com.aionn.identity.infrastructure.persistence.entity.UserConsentEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.ConsentResultMapper;
import com.aionn.identity.infrastructure.persistence.repository.consent.UserConsentRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String CONSENT_ID = "01HZCNS0000000000000000001";

    @Mock
    private UserConsentRepository consentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConsentResultMapper consentResultMapper;

    @InjectMocks
    private ConsentPersistenceAdapter adapter;

    private UserConsent consent(boolean granted, Instant revokedAt) {
        return UserConsent.builder()
                .id(CONSENT_ID)
                .userId(USER_ID)
                .consentType(ConsentType.MARKETING)
                .version("v1")
                .granted(granted)
                .revokedAt(revokedAt)
                .ipAddress("127.0.0.1")
                .build();
    }

    private ConsentResult result() {
        return new ConsentResult(CONSENT_ID, USER_ID, "MARKETING", "v1", true,
                Instant.now(), null, "127.0.0.1");
    }

    @Test
    void appendPersistsGrantedConsentWithoutRevokedAt() {
        UserConsent consent = consent(true, Instant.now());
        ConsentResult result = result();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(consentRepository.save(any(UserConsentEntity.class))).thenReturn(mock(UserConsentEntity.class));
        when(consentResultMapper.toResult(any(UserConsentEntity.class))).thenReturn(result);

        assertThat(adapter.append(consent)).isSameAs(result);

        ArgumentCaptor<UserConsentEntity> captor = ArgumentCaptor.forClass(UserConsentEntity.class);
        verify(consentRepository).save(captor.capture());
        assertThat(captor.getValue().getConsentType()).isEqualTo("MARKETING");
        assertThat(captor.getValue().getVersion()).isEqualTo("v1");
        assertThat(captor.getValue().getRevokedAt()).isNull();
    }

    @Test
    void appendPersistsRevokedAtWhenNotGranted() {
        Instant revokedAt = Instant.now();
        UserConsent consent = consent(false, revokedAt);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(consentRepository.save(any(UserConsentEntity.class))).thenReturn(mock(UserConsentEntity.class));
        when(consentResultMapper.toResult(any(UserConsentEntity.class))).thenReturn(result());

        adapter.append(consent);

        ArgumentCaptor<UserConsentEntity> captor = ArgumentCaptor.forClass(UserConsentEntity.class);
        verify(consentRepository).save(captor.capture());
        assertThat(captor.getValue().getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void appendThrowsWhenUserMissing() {
        UserConsent consent = consent(true, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.append(consent))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(consentRepository, never()).save(any());
    }

    @Test
    void findLatestReturnsMappedWhenPresent() {
        UserConsentEntity entity = mock(UserConsentEntity.class);
        ConsentResult result = result();
        when(consentRepository.findTopByUser_UserIdAndConsentTypeOrderByAgreedAtDesc(USER_ID, "MARKETING"))
                .thenReturn(Optional.of(entity));
        when(consentResultMapper.toResult(entity)).thenReturn(result);

        assertThat(adapter.findLatest(USER_ID, "MARKETING")).contains(result);
    }

    @Test
    void findLatestReturnsEmptyWhenMissing() {
        when(consentRepository.findTopByUser_UserIdAndConsentTypeOrderByAgreedAtDesc(USER_ID, "MARKETING"))
                .thenReturn(Optional.empty());

        assertThat(adapter.findLatest(USER_ID, "MARKETING")).isEmpty();
    }

    @Test
    void findHistoryDelegatesToMapper() {
        UserConsentEntity entity = mock(UserConsentEntity.class);
        List<ConsentResult> results = List.of(result());
        when(consentRepository.findByUser_UserIdOrderByAgreedAtDesc(USER_ID)).thenReturn(List.of(entity));
        when(consentResultMapper.toResults(List.of(entity))).thenReturn(results);

        assertThat(adapter.findHistory(USER_ID)).isEqualTo(results);
    }
}
