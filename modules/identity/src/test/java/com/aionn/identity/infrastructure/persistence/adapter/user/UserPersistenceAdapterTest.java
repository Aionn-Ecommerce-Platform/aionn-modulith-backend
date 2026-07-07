package com.aionn.identity.infrastructure.persistence.adapter.user;

import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.IdentityUserMapper;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserRepository userRepository;
    @Mock
    private IdentityUserMapper identityUserMapper;
    @Mock
    private IdentityUserEntitySaveSupport identityUserEntitySaveSupport;

    @InjectMocks
    private UserPersistenceAdapter adapter;

    private IdentityUser user() {
        return IdentityUser.createNew(USER_ID, "a@b.com", "+84912345678", "alice");
    }

    @Test
    void saveDelegatesToSaveSupport() {
        IdentityUser user = user();
        when(identityUserEntitySaveSupport.save(user)).thenReturn(user);

        assertThat(adapter.save(user)).isSameAs(user);
    }

    @Test
    void findByIdReturnsMappedWhenPresent() {
        UserEntity entity = UserEntity.builder().userId(USER_ID).build();
        IdentityUser user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(entity));
        when(identityUserMapper.toDomain(entity)).thenReturn(user);

        assertThat(adapter.findById(USER_ID)).contains(user);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(USER_ID)).isEmpty();
    }

    @Test
    void findByIdentityResolvesByEmail() {
        UserEntity entity = UserEntity.builder().userId(USER_ID).build();
        IdentityUser user = user();
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.of(entity));
        when(identityUserMapper.toDomain(entity)).thenReturn(user);

        assertThat(adapter.findByIdentity("id")).contains(user);
    }

    @Test
    void findByIdentityFallsBackToPhone() {
        UserEntity entity = UserEntity.builder().userId(USER_ID).build();
        IdentityUser user = user();
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("id")).thenReturn(Optional.of(entity));
        when(identityUserMapper.toDomain(entity)).thenReturn(user);

        assertThat(adapter.findByIdentity("id")).contains(user);
    }

    @Test
    void findByIdentityFallsBackToUsername() {
        UserEntity entity = UserEntity.builder().userId(USER_ID).build();
        IdentityUser user = user();
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("id")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("id")).thenReturn(Optional.of(entity));
        when(identityUserMapper.toDomain(entity)).thenReturn(user);

        assertThat(adapter.findByIdentity("id")).contains(user);
    }

    @Test
    void findByIdentityReturnsEmptyWhenNoMatch() {
        when(userRepository.findByEmailIgnoreCase("id")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("id")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("id")).thenReturn(Optional.empty());

        assertThat(adapter.findByIdentity("id")).isEmpty();
    }

    @Test
    void existsByPhoneDelegates() {
        when(userRepository.existsByPhone("+84912345678")).thenReturn(true);

        assertThat(adapter.existsByPhone("+84912345678")).isTrue();
    }

    @Test
    void existsByUsernameDelegates() {
        when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);

        assertThat(adapter.existsByUsername("alice")).isTrue();
    }

    @Test
    void existsByIdDelegates() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        assertThat(adapter.existsById(USER_ID)).isTrue();
    }
}
