package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.MerchantPayout;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayoutResultMapper {

    MerchantPayoutResult toResult(MerchantPayout payout);

    MerchantBalanceResult toResult(MerchantBalance balance);
}
