package com.aionn.identity.infrastructure.persistence.adapter.user;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.IdentityUserMapper;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserRepository userRepository;
    @Mock
    private IdentityUserMapper identityUserMapper;
    @Mock
    private IdentityUserEntitySaveSupport identityUserEntitySaveSupport;

    @InjectMocks
    private AdminUserPersistenceAdapter adapter;

    private IdentityUser user() {
        return IdentityUser.createNew(USER_ID, "a@b.com", "+84912345678", "alice");
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
    void saveDelegatesToSaveSupport() {
        IdentityUser user = user();
        when(identityUserEntitySaveSupport.save(user)).thenReturn(user);

        assertThat(adapter.save(user)).isSameAs(user);
    }

    @Test
    void findUsersWithFiltersMapsPage() {
        UserEntity entity = UserEntity.builder().userId(USER_ID).build();
        IdentityUser user = user();
        when(userRepository.findUsersWithFilters(eq(UserStatus.ACTIVE), eq(UserRole.BUYER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1));
        when(identityUserMapper.toDomain(entity)).thenReturn(user);

        PageResult<IdentityUser> result = adapter.findUsersWithFilters(
                UserStatus.ACTIVE, UserRole.BUYER, OffsetPagination.of(0, 10));

        assertThat(result.content()).containsExactly(user);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
