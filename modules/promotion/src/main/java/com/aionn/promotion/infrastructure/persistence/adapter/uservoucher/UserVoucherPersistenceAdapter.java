package com.aionn.promotion.infrastructure.persistence.adapter.uservoucher;

import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.infrastructure.persistence.entity.UserVoucherEntity;
import com.aionn.promotion.infrastructure.persistence.mapper.UserVoucherDomainMapper;
import com.aionn.promotion.infrastructure.persistence.repository.UserVoucherRepository;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    @Override
    public void lockUserClaim(String userId) {
        entityManager.createNativeQuery(
                "SELECT pg_advisory_xact_lock(hashtextextended(CAST(:userId AS text), 0))")
                .setParameter("userId", userId)
                .getSingleResult();
    }

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
    public Optional<UserVoucher> findByIdForUpdate(String userVoucherId) {
        return jpa.findByIdForUpdate(userVoucherId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserVoucher> findByUserAndCode(String userId, String voucherCode) {
        return jpa.findByUserIdAndVoucherCode(userId, voucherCode).map(mapper::toDomain);
    }

    @Override
    public Optional<UserVoucher> findByUserAndCodeForUpdate(String userId, String voucherCode) {
        return jpa.findByUserIdAndVoucherCodeForUpdate(userId, voucherCode).map(mapper::toDomain);
    }

    @Override
    public Optional<UserVoucher> findByUserAndCodeAndEntitlement(
            String userId, String voucherCode, String entitlementKey) {
        return jpa.findByUserIdAndVoucherCodeAndEntitlementKey(userId, voucherCode, entitlementKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserVoucher> findByReservedOrderId(String orderId) {
        return jpa.findByReservedOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserVoucher> findByReservedOrderIdForUpdate(String orderId) {
        return jpa.findByReservedOrderIdForUpdate(orderId).map(mapper::toDomain);
    }

    @Override
    public long countByUserAndCampaign(String userId, String campaignId) {
        return jpa.countByUserAndCampaign(userId, campaignId);
    }

    @Override
    public long countByUserClaimedSince(String userId, Instant since) {
        return jpa.countByUserIdAndClaimedAtGreaterThanEqual(userId, since);
    }

    @Override
    public long countActiveByUser(String userId, java.time.Instant now) {
        return jpa.countActiveByUserId(userId, now);
    }

    @Override
    public List<UserVoucher> findByUser(String userId, int limit) {
        return jpa.findByUserIdOrderByClaimedAtDesc(userId, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserVoucher> searchByUser(String userId, List<String> statuses, int page, int limit) {
        boolean hasStatuses = statuses != null && !statuses.isEmpty();
        return jpa.searchByUserId(
                        userId,
                        hasStatuses,
                        hasStatuses ? statuses : List.of("__NONE__"),
                        PageRequest.of(Math.max(0, page), Math.max(1, limit)))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countSearchByUser(String userId, List<String> statuses) {
        boolean hasStatuses = statuses != null && !statuses.isEmpty();
        return jpa.countSearchByUserId(
                userId,
                hasStatuses,
                hasStatuses ? statuses : List.of("__NONE__"));
    }

    @Override
    public List<UserVoucher> findExpiredReservations(Instant now, int limit) {
        return jpa.findExpiredReservations(now, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
