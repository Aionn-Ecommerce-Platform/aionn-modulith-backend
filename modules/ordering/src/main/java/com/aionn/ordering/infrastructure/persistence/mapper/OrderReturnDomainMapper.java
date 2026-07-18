package com.aionn.ordering.infrastructure.persistence.mapper;

import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.ordering.infrastructure.persistence.entity.OrderReturnEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderReturnDomainMapper {

    public OrderReturn toDomain(OrderReturnEntity e) {
        Money refund = (e.getRefundAmount() != null && e.getRefundCurrency() != null)
                ? Money.of(e.getRefundAmount(), e.getRefundCurrency())
                : null;
        return new OrderReturn(
                e.getReturnId(),
                e.getOrderId(),
                e.getUserId(),
                e.getMerchantId(),
                e.getReason(),
                e.getEvidenceUrl(),
                refund,
                e.getReturnWarehouseId(),
                e.getItemCondition(),
                e.getRejectionReason(),
                ReturnStatus.valueOf(e.getStatus()),
                e.getRequestedAt(),
                e.getDecidedAt(),
                e.getReceivedAt());
    }

    public OrderReturnEntity toEntity(OrderReturn r, OrderReturnEntity existing) {
        OrderReturnEntity entity = existing != null ? existing
                : OrderReturnEntity.builder()
                        .returnId(r.getReturnId())
                        .orderId(r.getOrderId())
                        .userId(r.getUserId())
                        .merchantId(r.getMerchantId())
                        .reason(r.getReason())
                        .evidenceUrl(r.getEvidenceUrl())
                        .build();
        entity.setRefundAmount(r.getRefundAmount() == null ? null : r.getRefundAmount().amount());
        entity.setRefundCurrency(r.getRefundAmount() == null ? null : r.getRefundAmount().currency());
        entity.setReturnWarehouseId(r.getReturnWarehouseId());
        entity.setItemCondition(r.getItemCondition());
        entity.setRejectionReason(r.getRejectionReason());
        entity.setStatus(r.getStatus().name());
        entity.setDecidedAt(r.getDecidedAt());
        entity.setReceivedAt(r.getReceivedAt());
        return entity;
    }
}

