package com.aionn.identity.infrastructure.persistence.adapter.preference;

import com.aionn.identity.application.dto.preference.result.UserPreferenceResult;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserPreferenceEntity;
import com.aionn.identity.infrastructure.persistence.mapper.UserPreferenceDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.preference.UserPreferenceRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferencePersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserPreferenceRepository preferenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPreferenceDomainMapper mapper;

    @InjectMocks
    private UserPreferencePersistenceAdapter adapter;

    private UserPreferenceResult result() {
        return new UserPreferenceResult(USER_ID, "vi", "VND", "Asia/Ho_Chi_Minh", "dark",
                "{}", "{}", Instant.now());
    }

    @Test
    void saveAttachesUserAndMapsBack() {
        UserPreferenceResult preference = result();
        UserPreferenceEntity entity = mock(UserPreferenceEntity.class);
        UserEntity user = UserEntity.builder().userId(USER_ID).build();
        when(mapper.toEntity(preference)).thenReturn(entity);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(preferenceRepository.save(entity)).thenReturn(entity);
        when(mapper.toResult(entity)).thenReturn(preference);

        assertThat(adapter.save(preference)).isSameAs(preference);
        verify(entity).setUser(user);
    }

    @Test
    void saveThrowsWhenUserMissing() {
        UserPreferenceResult preference = result();
        when(mapper.toEntity(preference)).thenReturn(mock(UserPreferenceEntity.class));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(preference))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsMappedWhenPresent() {
        UserPreferenceEntity entity = mock(UserPreferenceEntity.class);
        UserPreferenceResult preference = result();
        when(preferenceRepository.findById(USER_ID)).thenReturn(Optional.of(entity));
        when(mapper.toResult(entity)).thenReturn(preference);

        assertThat(adapter.findById(USER_ID)).contains(preference);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(preferenceRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(USER_ID)).isEmpty();
    }

    @Test
    void createDefaultPersistsNewPreferenceForUser() {
        UserPreferenceResult preference = result();
        UserPreferenceEntity saved = mock(UserPreferenceEntity.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(UserEntity.builder().userId(USER_ID).build()));
        when(preferenceRepository.save(any(UserPreferenceEntity.class))).thenReturn(saved);
        when(mapper.toResult(saved)).thenReturn(preference);

        assertThat(adapter.createDefault(USER_ID)).isSameAs(preference);
    }

    @Test
    void createDefaultThrowsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.createDefault(USER_ID))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(preferenceRepository, never()).save(any());
    }
}
