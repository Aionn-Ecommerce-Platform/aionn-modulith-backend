package com.aionn.identity.infrastructure.persistence.adapter.user;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.port.out.admin.AdminUserPersistencePort;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.mapper.IdentityUserMapper;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminUserPersistenceAdapter implements AdminUserPersistencePort {

    private final UserRepository userRepository;
    private final IdentityUserMapper identityUserMapper;
    private final IdentityUserEntitySaveSupport identityUserEntitySaveSupport;

    @Override
    public Optional<IdentityUser> findById(String userId) {
        return userRepository.findById(userId)
                .map(identityUserMapper::toDomain);
    }

    @Override
    public IdentityUser save(IdentityUser user) {
        return identityUserEntitySaveSupport.save(user);
    }

    @Override
    public PageResult<IdentityUser> findUsersWithFilters(UserStatus status, UserRole role,
            OffsetPagination pagination) {
        Pageable pageable = PageRequest.of(pagination.page(), pagination.size());
        Page<IdentityUser> page = userRepository.findUsersWithFilters(status, role, pageable)
                .map(identityUserMapper::toDomain);
        List<IdentityUser> content = page.getContent();
        return new PageResult<>(content, pagination.page(), pagination.size(), page.getTotalElements());
    }
}
