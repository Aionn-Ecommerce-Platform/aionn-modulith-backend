package com.aionn.payment.infrastructure.persistence.adapter.settlement;

import com.aionn.payment.application.port.out.MerchantBalanceQueryPort;
import com.aionn.payment.infrastructure.persistence.repository.MerchantBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MerchantBalanceQueryAdapter implements MerchantBalanceQueryPort {

    private final MerchantBalanceRepository jpa;

    @Override
    @Transactional(readOnly = true)
    public List<EligibleBalance> findEligibleForAutoPayout(BigDecimal minAvailable, String currency, int limit) {
        return jpa.findEligibleForAutoPayout(minAvailable, currency,
                PageRequest.of(0, Math.max(1, limit))).stream()
                .map(b -> new EligibleBalance(b.getMerchantId(), b.getCurrency(), b.getAvailable()))
                .toList();
    }
}
