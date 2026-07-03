package com.aionn.identity.application.port.out.admin;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.Optional;

public interface AdminUserPersistencePort {

    Optional<IdentityUser> findById(String userId);

    IdentityUser save(IdentityUser user);

    PageResult<IdentityUser> findUsersWithFilters(UserStatus status, UserRole role, OffsetPagination pagination);
}
