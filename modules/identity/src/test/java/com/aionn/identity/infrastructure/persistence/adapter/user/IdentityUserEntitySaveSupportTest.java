package com.aionn.identity.infrastructure.persistence.adapter.user;

import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.IdentityUserMapper;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityUserEntitySaveSupportTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserRepository userRepository;
    @Mock
    private IdentityUserMapper identityUserMapper;

    @InjectMocks
    private IdentityUserEntitySaveSupport saveSupport;

    private IdentityUser domainUser() {
        return new IdentityUser(
                USER_ID, "new@b.com", "+84912345678", "alice", "hash", "Alice", "avatar",
                Set.of(UserRole.MERCHANT), UserStatus.ACTIVE,
                LocalDateTime.now(), null, null, LocalDateTime.now());
    }

    @Test
    void saveUpdatesExistingEntityInPlace() {
        UserEntity existing = UserEntity.builder()
                .userId(USER_ID)
                .email("old@b.com")
                .roles(new LinkedHashSet<>(Set.of(UserRole.BUYER)))
                .build();
        IdentityUser user = domainUser();
        IdentityUser mapped = user;
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(identityUserMapper.toDomain(existing)).thenReturn(mapped);

        IdentityUser result = saveSupport.save(user);

        assertThat(result).isSameAs(mapped);
        assertThat(existing.getEmail()).isEqualTo("new@b.com");
        assertThat(existing.getDisplayName()).isEqualTo("Alice");
        assertThat(existing.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(existing.getRoles()).containsExactly(UserRole.MERCHANT);
    }

    @Test
    void saveCreatesNewEntityWhenUserNotFound() {
        IdentityUser user = domainUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(identityUserMapper.toDomain(any(UserEntity.class))).thenReturn(user);

        IdentityUser result = saveSupport.save(user);

        assertThat(result).isSameAs(user);
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity persisted = captor.getValue();
        assertThat(persisted.getUserId()).isEqualTo(USER_ID);
        assertThat(persisted.getEmail()).isEqualTo("new@b.com");
        assertThat(persisted.getRoles()).containsExactly(UserRole.MERCHANT);
    }
}

