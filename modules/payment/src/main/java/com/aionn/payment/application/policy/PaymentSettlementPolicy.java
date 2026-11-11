package com.aionn.payment.application.policy;

import java.math.BigDecimal;

public interface PaymentSettlementPolicy {

    BigDecimal defaultCommissionRate();
}
