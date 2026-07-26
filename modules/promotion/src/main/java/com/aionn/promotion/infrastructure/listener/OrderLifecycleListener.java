package com.aionn.promotion.infrastructure.listener;

import com.aionn.promotion.application.service.VoucherService;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLifecycleListener {

    private final VoucherService voucherService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCancelled(OrderCancelledIntegrationEvent event) {
        String reason = event.cancellationType().name() + ":" + event.reasonCode();
        releaseVouchers(event.orderId(), reason);
    }

    private void releaseVouchers(String orderId, String reason) {
        try {
            int released = voucherService.releaseByOrder(orderId, reason);
            if (released > 0) {
                log.info("Released {} voucher reservation(s) for cancelled order {}", released, orderId);
            }
        } catch (Exception ex) {
            log.warn("Failed to release vouchers for order {}: {}", orderId, ex.getMessage());
        }
    }
}
