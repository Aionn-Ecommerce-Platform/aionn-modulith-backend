package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.UserVoucher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserVoucherPersistencePort {

    UserVoucher save(UserVoucher userVoucher);

    Optional<UserVoucher> findById(String userVoucherId);

    Optional<UserVoucher> findByUserAndCode(String userId, String voucherCode);

    Optional<UserVoucher> findByReservedOrderId(String orderId);

    long countByUserAndCampaign(String userId, String campaignId);

    List<UserVoucher> findByUser(String userId, int limit);

    List<UserVoucher> findExpiredReservations(Instant now, int limit);
}
