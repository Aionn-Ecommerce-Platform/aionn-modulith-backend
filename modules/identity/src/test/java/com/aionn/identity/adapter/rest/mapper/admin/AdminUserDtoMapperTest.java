package com.aionn.identity.adapter.rest.mapper.admin;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUserDtoMapperTest {

    private final AdminUserDtoMapper mapper = new AdminUserDtoMapperImpl();

    @Test
    void createsListQueryWhenOptionalFiltersAreMissing() {
        var query = mapper.toListUsersQuery(null, null, 0, 20);

        assertThat(query).isNotNull();
        assertThat(query.status()).isNull();
        assertThat(query.role()).isNull();
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void preservesListFiltersAndPagination() {
        var query = mapper.toListUsersQuery(UserStatus.SUSPENDED, UserRole.MERCHANT, 2, 50);

        assertThat(query.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(query.role()).isEqualTo(UserRole.MERCHANT);
        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(50);
    }
}
