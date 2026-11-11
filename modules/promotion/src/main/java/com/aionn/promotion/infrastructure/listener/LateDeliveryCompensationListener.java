package com.aionn.promotion.infrastructure.listener;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.port.in.voucher.GrantVoucherInputPort;
import com.aionn.contracts.event.shipping.ShipmentDeliveredIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("promotionLateDeliveryCompensationListener")
@RequiredArgsConstructor
public class LateDeliveryCompensationListener {

    public static final String LATE_DELIVERY_VOUCHER_CODE = "LATEDELIVERY15K";

    private final GrantVoucherInputPort grantVoucherInputPort;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onShipmentDelivered(ShipmentDeliveredIntegrationEvent event) {
        if (!event.lateDelivery()) {
            return;
        }

        if (event.userId() == null || event.userId().isBlank()) {
            log.warn("Late delivery detected for shipment {} (order {}), but userId is missing; skipping compensation.",
                    event.shipmentId(), event.orderId());
            return;
        }

        log.info("Late delivery detected for shipment {} (order {}), user {}. Granting compensation voucher {}",
                event.shipmentId(), event.orderId(), event.userId(), LATE_DELIVERY_VOUCHER_CODE);

        grantVoucherInputPort.execute(new VoucherCommands.GrantVoucher(
                event.userId(),
                LATE_DELIVERY_VOUCHER_CODE,
                "Late delivery compensation for order " + event.orderId(),
                "order-" + event.orderId()
        ));
        log.info("Successfully granted late delivery voucher {} to user {} for order {}",
                LATE_DELIVERY_VOUCHER_CODE, event.userId(), event.orderId());
    }
}

