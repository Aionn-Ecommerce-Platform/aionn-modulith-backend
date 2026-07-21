package com.aionn.payment.application.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface MerchantBalanceQueryPort {

    List<EligibleBalance> findEligibleForAutoPayout(BigDecimal minAvailable, String currency, int limit);

    record EligibleBalance(String merchantId, String currency, BigDecimal available) {
    }
}
