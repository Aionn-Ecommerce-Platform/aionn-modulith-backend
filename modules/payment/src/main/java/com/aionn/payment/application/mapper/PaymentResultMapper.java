package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.ledger.result.LedgerResult;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.model.TransactionLedger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentResultMapper {

    @Mapping(target = "amount", source = "amount.amount")
    @Mapping(target = "refundedAmount", source = "refundedAmount.amount")
    @Mapping(target = "currency", source = "amount.currency")
    @Mapping(target = "redirectUrl", ignore = true)
    @Mapping(target = "withRedirectUrl", ignore = true)
    PaymentResult toResult(Payment payment);

    PaymentMethodResult toResult(PaymentMethod method);

    @Mapping(target = "amount", source = "amount.amount")
    @Mapping(target = "currency", source = "amount.currency")
    LedgerResult toResult(TransactionLedger ledger);
}
