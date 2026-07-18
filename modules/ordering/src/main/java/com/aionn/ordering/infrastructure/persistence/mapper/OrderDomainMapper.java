package com.aionn.ordering.infrastructure.persistence.mapper;

import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import com.aionn.ordering.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderDomainMapper {

    public Order toDomain(OrderEntity e) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemEntity item : e.getItems()) {
            items.add(new OrderItem(
                    item.getId().getSkuId(),
                    item.getQty(),
                    Money.of(item.getUnitPrice(), e.getCurrency()),
                    item.getWarehouseId(),
                    item.getReservationId()));
        }
        boolean hasAddress = e.getAddressId() != null
                || e.getAddressFullName() != null
                || e.getAddressLine() != null;
        ShippingAddress address = !hasAddress ? null
                : new ShippingAddress(
                        e.getAddressId(),
                        e.getAddressFullName(),
                        e.getAddressPhone(),
                        e.getAddressLine(),
                        e.getAddressWardCode(),
                        e.getAddressDistrictCode(),
                        e.getAddressProvinceCode(),
                        e.getAddressCountryCode());
        Money shippingFee = e.getShippingFee() == null ? null : Money.of(e.getShippingFee(), e.getCurrency());
        Money total = e.getTotalAmount() == null ? null : Money.of(e.getTotalAmount(), e.getCurrency());
        return new Order(
                e.getOrderId(),
                e.getParentOrderId(),
                e.getUserId(),
                e.getMerchantId(),
                e.getProposalId(),
                e.getPaymentMethodId(),
                e.getCurrency(),
                items,
                address,
                shippingFee,
                total,
                OrderStatus.valueOf(e.getStatus()),
                e.getPaymentId(),
                e.getReasonCode(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getCompletedAt(),
                e.getCancelledAt());
    }

    public OrderEntity toEntity(Order order, OrderEntity existing) {
        OrderEntity entity = existing != null ? existing
                : OrderEntity.builder()
                        .orderId(order.getOrderId())
                        .userId(order.getUserId())
                        .merchantId(order.getMerchantId())
                        .proposalId(order.getProposalId())
                        .currency(order.getCurrency())
                        .build();
        entity.setParentOrderId(order.getParentOrderId());
        entity.setPaymentMethodId(order.getPaymentMethodId());
        entity.setPaymentId(order.getPaymentId());
        entity.setTotalAmount(order.getTotalAmount() == null ? null : order.getTotalAmount().amount());
        entity.setShippingFee(order.getShippingFee() == null ? null : order.getShippingFee().amount());
        entity.setStatus(order.getStatus().name());
        entity.setReasonCode(order.getReasonCode());
        entity.setCompletedAt(order.getCompletedAt());
        entity.setCancelledAt(order.getCancelledAt());

        ShippingAddress address = order.getShippingAddress();
        if (address != null) {
            entity.setAddressId(address.addressId());
            entity.setAddressFullName(address.fullName());
            entity.setAddressPhone(address.phone());
            entity.setAddressLine(address.addressLine());
            entity.setAddressWardCode(address.wardCode());
            entity.setAddressDistrictCode(address.districtCode());
            entity.setAddressProvinceCode(address.provinceCode());
            entity.setAddressCountryCode(address.countryCode());
        }

        entity.getItems().clear();
        for (OrderItem item : order.items()) {
            OrderItemEntity itemEntity = OrderItemEntity.builder()
                    .id(new OrderItemEntity.OrderItemId(order.getOrderId(), item.skuId()))
                    .order(entity)
                    .qty(item.qty())
                    .unitPrice(item.unitPrice().amount())
                    .warehouseId(item.warehouseId())
                    .reservationId(item.reservationId())
                    .build();
            entity.getItems().add(itemEntity);
        }
        return entity;
    }
}

