package com.aionn.arch;

import static org.assertj.core.api.Assertions.assertThat;

import com.aionn.identity.infrastructure.scheduling.AuthSessionCleanupScheduler;
import com.aionn.inventory.infrastructure.scheduling.ReservationAutoReleaseScheduler;
import com.aionn.notification.infrastructure.scheduling.NotificationRetryScheduler;
import com.aionn.ordering.infrastructure.scheduling.OrderAutoCancelScheduler;
import com.aionn.payment.infrastructure.scheduling.AutoPayoutScheduler;
import com.aionn.promotion.infrastructure.scheduling.CampaignStatusScheduler;
import com.aionn.promotion.infrastructure.scheduling.VoucherAutoReleaseScheduler;
import com.aionn.shipping.infrastructure.scheduling.ShipmentStatusPollScheduler;
import java.lang.reflect.Method;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class SchedulerLockArchitectureTest {

    @Test
    void everySingletonBusinessSchedulerHasADistributedLock() {
        List<Class<?>> schedulers = List.of(
                AuthSessionCleanupScheduler.class,
                ReservationAutoReleaseScheduler.class,
                OrderAutoCancelScheduler.class,
                AutoPayoutScheduler.class,
                ShipmentStatusPollScheduler.class,
                CampaignStatusScheduler.class,
                VoucherAutoReleaseScheduler.class,
                NotificationRetryScheduler.class);

        for (Class<?> scheduler : schedulers) {
            List<Method> scheduledMethods = List.of(scheduler.getDeclaredMethods()).stream()
                    .filter(method -> method.isAnnotationPresent(Scheduled.class))
                    .toList();
            assertThat(scheduledMethods).as(scheduler.getSimpleName()).hasSize(1);
            assertThat(scheduledMethods.getFirst().isAnnotationPresent(SchedulerLock.class))
                    .as(scheduler.getSimpleName() + " distributed lock")
                    .isTrue();
        }
    }
}
