package com.aionn.promotion.infrastructure.listener;

import com.aionn.contracts.event.catalog.MerchantFollowerNotificationIntegrationEvent;
import com.aionn.contracts.publisher.IntegrationEventPublisher;
import com.aionn.promotion.domain.event.PromotionEvents;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantFollowerPromotionListener {

    private static final String TYPE_SHOP_VOUCHER = "SHOP_VOUCHER";
    private static final String TYPE_FLASH_SALE = "FLASH_SALE";

    private final IntegrationEventPublisher integrationEventPublisher;

    @EventListener
    public void onVoucherIssued(PromotionEvents.VoucherIssued event) {
        if (event.merchantId() == null || event.merchantId().isBlank()) {
            return;
        }
        integrationEventPublisher.publish(new MerchantFollowerNotificationIntegrationEvent(
                IdGenerator.ulid(),
                event.merchantId(),
                TYPE_SHOP_VOUCHER,
                "Voucher mới từ cửa hàng",
                "Mã giảm giá " + event.voucherCode() + " vừa được phát hành",
                "/merchants/" + event.merchantId(),
                event.occurredAt()));
    }

    @EventListener
    public void onFlashSaleApproved(PromotionEvents.FlashSaleApproved event) {
        if (event.merchantId() == null || event.merchantId().isBlank()) {
            return;
        }
        integrationEventPublisher.publish(new MerchantFollowerNotificationIntegrationEvent(
                IdGenerator.ulid(),
                event.merchantId(),
                TYPE_FLASH_SALE,
                "Flash sale mới từ cửa hàng",
                "Sản phẩm vừa được duyệt vào flash sale",
                "/products/" + event.productId(),
                event.occurredAt()));
    }
}
