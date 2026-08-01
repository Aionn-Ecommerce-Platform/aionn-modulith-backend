package com.aionn.arch;

import com.aionn.chat.infrastructure.persistence.entity.ConversationEntity;
import com.aionn.chat.infrastructure.persistence.entity.MessageEntity;
import com.aionn.chat.infrastructure.persistence.entity.UserBlockEntity;
import com.aionn.inventory.infrastructure.persistence.entity.StockReservationEntity;
import com.aionn.inventory.infrastructure.persistence.entity.StockTransferEntity;
import com.aionn.notification.infrastructure.persistence.entity.NotificationEntity;
import com.aionn.ordering.infrastructure.persistence.entity.CartEntity;
import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import com.aionn.ordering.infrastructure.persistence.entity.OrderReturnEntity;
import com.aionn.payment.infrastructure.persistence.entity.MerchantBalanceEntity;
import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import com.aionn.promotion.infrastructure.persistence.entity.UserVoucherEntity;
import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEntity;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimisticLockingArchitectureTest {

    @Test
    void concurrentWorkflowEntitiesDeclareJpaVersionFields() {
        List<Class<?>> workflowEntities = List.of(
                CartEntity.class,
                OrderEntity.class,
                OrderReturnEntity.class,
                StockReservationEntity.class,
                StockTransferEntity.class,
                ShipmentEntity.class,
                MerchantBalanceEntity.class,
                MerchantPayoutEntity.class,
                UserVoucherEntity.class,
                NotificationEntity.class,
                ConversationEntity.class,
                MessageEntity.class,
                UserBlockEntity.class);

        for (Class<?> entity : workflowEntities) {
            assertTrue(hasVersionField(entity), () -> entity.getName() + " must declare a @Version field");
        }
    }

    private static boolean hasVersionField(Class<?> entity) {
        for (Field field : entity.getDeclaredFields()) {
            if (field.isAnnotationPresent(Version.class)) {
                return true;
            }
        }
        return false;
    }
}
