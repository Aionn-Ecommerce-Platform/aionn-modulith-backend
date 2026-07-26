package com.aionn.promotion.infrastructure.persistence.mapper;

import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.promotion.infrastructure.persistence.entity.UserVoucherEntity;
import org.springframework.stereotype.Component;

@Component
public class UserVoucherDomainMapper {

    public UserVoucher toDomain(UserVoucherEntity e) {
        Money applied = (e.getAppliedAmount() != null && e.getAppliedCurrency() != null)
                ? Money.of(e.getAppliedAmount(), e.getAppliedCurrency())
                : null;
        return new UserVoucher(
                e.getUserVoucherId(),
                e.getVoucherCode(),
                e.getUserId(),
                UserVoucherStatus.valueOf(e.getStatus()),
                e.getReservedOrderId(),
                applied,
                e.getClaimedAt(),
                e.getReservedAt(),
                e.getReservedExpiresAt(),
                e.getAppliedAt(),
                e.getReleasedAt(),
                e.getUpdatedAt());
    }

    public UserVoucherEntity toEntity(UserVoucher uv, UserVoucherEntity existing) {
        UserVoucherEntity entity = existing != null ? existing
                : UserVoucherEntity.builder()
                        .userVoucherId(uv.getUserVoucherId())
                        .voucherCode(uv.getVoucherCode())
                        .userId(uv.getUserId())
                        .build();
        entity.setStatus(uv.getStatus().name());
        entity.setReservedOrderId(uv.getReservedOrderId());
        entity.setAppliedAmount(uv.getAppliedAmount() == null ? null : uv.getAppliedAmount().amount());
        entity.setAppliedCurrency(uv.getAppliedAmount() == null ? null : uv.getAppliedAmount().currency());
        entity.setReservedAt(uv.getReservedAt());
        entity.setReservedExpiresAt(uv.getReservedExpiresAt());
        entity.setAppliedAt(uv.getAppliedAt());
        entity.setReleasedAt(uv.getReleasedAt());
        return entity;
    }
}

