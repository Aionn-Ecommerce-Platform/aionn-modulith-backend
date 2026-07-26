package com.aionn.promotion.infrastructure.persistence.adapter.uservoucher;

import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.infrastructure.persistence.entity.UserVoucherEntity;
import com.aionn.promotion.infrastructure.persistence.mapper.UserVoucherDomainMapper;
import com.aionn.promotion.infrastructure.persistence.repository.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserVoucherPersistenceAdapter implements UserVoucherPersistencePort {

    private final UserVoucherRepository jpa;
    private final UserVoucherDomainMapper mapper;

    @Override
    public UserVoucher save(UserVoucher userVoucher) {
        UserVoucherEntity existing = jpa.findById(userVoucher.getUserVoucherId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(userVoucher, existing)));
    }

    @Override
    public Optional<UserVoucher> findById(String userVoucherId) {
        return jpa.findById(userVoucherId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserVoucher> findByUserAndCode(String userId, String voucherCode) {
        return jpa.findByUserIdAndVoucherCode(userId, voucherCode).map(mapper::toDomain);
    }

    @Override
    public Optional<UserVoucher> findByReservedOrderId(String orderId) {
        return jpa.findByReservedOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public long countByUserAndCampaign(String userId, String campaignId) {
        return jpa.countByUserAndCampaign(userId, campaignId);
    }

    @Override
    public List<UserVoucher> findByUser(String userId, int limit) {
        return jpa.findByUserIdOrderByClaimedAtDesc(userId, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserVoucher> findExpiredReservations(Instant now, int limit) {
        return jpa.findExpiredReservations(now, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
