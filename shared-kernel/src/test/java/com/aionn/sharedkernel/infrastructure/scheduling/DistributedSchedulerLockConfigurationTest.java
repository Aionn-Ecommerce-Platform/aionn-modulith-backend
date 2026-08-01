package com.aionn.sharedkernel.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class DistributedSchedulerLockConfigurationTest {

    @Test
    void createsJdbcLockProvider() {
        var configuration = new DistributedSchedulerLockConfiguration();

        assertThat(configuration.schedulerLockProvider(mock(DataSource.class))).isNotNull();
    }
}
