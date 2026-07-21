package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.infrastructure.config.OrderingProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAutoCancelSchedulerTest {

        @Mock
        private OrderPersistencePort orderRepository;

        @Mock
        private OrderAutoCancelWorker worker;

        @InjectMocks
        private OrderAutoCancelScheduler scheduler;

        private OrderingProperties createPropertiesWithAutoCancel(int timeoutMinutes, int batchSize) {
                OrderingProperties.Reservation reservation = new OrderingProperties.Reservation(86400);
                OrderingProperties.AutoCancel autoCancel = new OrderingProperties.AutoCancel(
                                true, timeoutMinutes, 60000L, batchSize);
                return new OrderingProperties(reservation, autoCancel);
        }

        @Test
        void cancelsExpiredOrdersSuccessfully() {
                OrderingProperties properties = createPropertiesWithAutoCancel(30, 10);
                OrderAutoCancelScheduler scheduler = new OrderAutoCancelScheduler(
                                orderRepository, worker, properties);

                when(orderRepository.findPendingOrderIdsOlderThan(any(Instant.class), eq(10)))
                                .thenReturn(List.of("order-1", "order-2", "order-3"));

                scheduler.run();

                verify(worker).cancelOneExpired("order-1");
                verify(worker).cancelOneExpired("order-2");
                verify(worker).cancelOneExpired("order-3");
        }

        @Test
        void calculatesCutoffBasedOnTimeoutMinutes() {
                OrderingProperties properties = createPropertiesWithAutoCancel(60, 50);
                OrderAutoCancelScheduler scheduler = new OrderAutoCancelScheduler(
                                orderRepository, worker, properties);

                when(orderRepository.findPendingOrderIdsOlderThan(any(Instant.class), anyInt()))
                                .thenReturn(List.of());

                Instant beforeRun = Instant.now().minusSeconds(3660); // ~61 minutes ago
                scheduler.run();
                Instant afterRun = Instant.now().minusSeconds(3540); // ~59 minutes ago

                ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
                verify(orderRepository).findPendingOrderIdsOlderThan(cutoffCaptor.capture(), eq(50));

                Instant actualCutoff = cutoffCaptor.getValue();
                assertThat(actualCutoff).isBetween(beforeRun, afterRun);
        }

        @Test
        void handlesOptimisticLockingFailureGracefully() {
                OrderingProperties properties = createPropertiesWithAutoCancel(30, 10);
                OrderAutoCancelScheduler scheduler = new OrderAutoCancelScheduler(
                                orderRepository, worker, properties);

                when(orderRepository.findPendingOrderIdsOlderThan(any(Instant.class), anyInt()))
                                .thenReturn(List.of("order-1", "order-2"));
                doThrow(new OptimisticLockingFailureException("Concurrent modification"))
                                .when(worker).cancelOneExpired("order-1");

                scheduler.run();

                verify(worker).cancelOneExpired("order-1");
                verify(worker).cancelOneExpired("order-2"); // Continues despite failure
        }

        @Test
        void handlesWorkerRuntimeExceptionGracefully() {
                OrderingProperties properties = createPropertiesWithAutoCancel(30, 10);
                OrderAutoCancelScheduler scheduler = new OrderAutoCancelScheduler(
                                orderRepository, worker, properties);

                when(orderRepository.findPendingOrderIdsOlderThan(any(Instant.class), anyInt()))
                                .thenReturn(List.of("order-1", "order-2", "order-3"));
                doNothing().when(worker).cancelOneExpired("order-1");
                doThrow(new RuntimeException("Unexpected error"))
                                .when(worker).cancelOneExpired("order-2");
                doNothing().when(worker).cancelOneExpired("order-3");

                scheduler.run();

                verify(worker).cancelOneExpired("order-1");
                verify(worker).cancelOneExpired("order-2");
                verify(worker).cancelOneExpired("order-3"); // Continues despite failure
        }

        @Test
        void doesNothingWhenNoPendingOrdersFound() {
                OrderingProperties properties = createPropertiesWithAutoCancel(30, 10);
                OrderAutoCancelScheduler scheduler = new OrderAutoCancelScheduler(
                                orderRepository, worker, properties);

                when(orderRepository.findPendingOrderIdsOlderThan(any(Instant.class), anyInt()))
                                .thenReturn(List.of());

                scheduler.run();

                verify(worker, never()).cancelOneExpired(anyString());
        }

        @Test
        void handlesRepositoryExceptionGracefully() {
                OrderingProperties properties = createPropertiesWithAutoCancel(30, 10);
                OrderAutoCancelScheduler scheduler = new OrderAutoCancelScheduler(
                                orderRepository, worker, properties);

                when(orderRepository.findPendingOrderIdsOlderThan(any(Instant.class), anyInt()))
                                .thenThrow(new RuntimeException("Database error"));

                // Should not throw exception
                scheduler.run();

                verify(worker, never()).cancelOneExpired(anyString());
        }

        @Test
        void usesBatchSizeFromProperties() {
                OrderingProperties properties = createPropertiesWithAutoCancel(30, 25);
                OrderAutoCancelScheduler scheduler = new OrderAutoCancelScheduler(
                                orderRepository, worker, properties);

                when(orderRepository.findPendingOrderIdsOlderThan(any(Instant.class), eq(25)))
                                .thenReturn(List.of());

                scheduler.run();

                verify(orderRepository).findPendingOrderIdsOlderThan(any(Instant.class), eq(25));
        }
}
