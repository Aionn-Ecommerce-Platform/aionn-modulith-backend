package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.application.port.out.consent.ConsentPersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.model.UserConsent;
import com.aionn.identity.domain.valueobject.ConsentType;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private ConsentPersistencePort consentPersistencePort;

    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        consentService = new ConsentService(userPersistencePort, consentPersistencePort, Clock.systemUTC());
    }

    @Test
    void agreeTermsAppendsGrantedConsent() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(consentPersistencePort.append(any())).thenAnswer(invocation -> {
            UserConsent input = invocation.getArgument(0);
            return new ConsentResult(input.getId(), input.getUserId(), input.getConsentType().name(),
                    input.getVersion(), input.isGranted(), input.getAgreedAt(), input.getRevokedAt(),
                    input.getIpAddress());
        });

        ConsentResult result = consentService.agreeTerms(USER_ID, "v1", "1.1.1.1");

        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(consentPersistencePort).append(captor.capture());
        UserConsent persisted = captor.getValue();
        assertThat(persisted.getConsentType()).isEqualTo(ConsentType.TERMS);
        assertThat(persisted.getVersion()).isEqualTo("v1");
        assertThat(persisted.getUserId()).isEqualTo(USER_ID);
        assertThat(persisted.getAgreedAt()).isNotNull();
        assertThat(persisted.getRevokedAt()).isNull();
        assertThat(result).isNotNull();
    }

    @Test
    void updateMarketingRevokedSetsRevokedAt() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(consentPersistencePort.append(any())).thenAnswer(invocation -> {
            UserConsent input = invocation.getArgument(0);
            return new ConsentResult(input.getId(), input.getUserId(), input.getConsentType().name(),
                    input.getVersion(), input.isGranted(), input.getAgreedAt(), input.getRevokedAt(),
                    input.getIpAddress());
        });

        consentService.updateMarketing(USER_ID, false, "1.1.1.1");

        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(consentPersistencePort).append(captor.capture());
        UserConsent persisted = captor.getValue();
        assertThat(persisted.getConsentType()).isEqualTo(ConsentType.MARKETING);
        assertThat(persisted.getVersion()).isEqualTo(ConsentService.DEFAULT_MARKETING_VERSION);
        assertThat(persisted.getRevokedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidIpAddress() {
        var ex = assertThrows(IdentityException.class,
                () -> consentService.agreeTerms(USER_ID, "v1", "not-an-ip"));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.INVALID_IP_ADDRESS.getCode());
    }

    @Test
    void rejectsMissingUser() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class,
                () -> consentService.agreePrivacy(USER_ID, "v1", "1.1.1.1"));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void listMyDelegatesToPort() {
        when(userPersistencePort.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        ConsentResult one = new ConsentResult("c1", USER_ID, "TERMS", "v1", true,
                Instant.now(), null, "1.1.1.1");
        when(consentPersistencePort.findHistory(USER_ID)).thenReturn(List.of(one));

        List<ConsentResult> result = consentService.listMy(USER_ID);

        assertThat(result).isEqualTo(List.of(one));
    }

    private static IdentityUser activeUser() {
        return new IdentityUser(
                USER_ID, "alice@example.com", "+84912345678", "alice",
                "hash", "Alice", null,
                Set.of(UserRole.BUYER), UserStatus.ACTIVE,
                null, null, null, Instant.now());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
