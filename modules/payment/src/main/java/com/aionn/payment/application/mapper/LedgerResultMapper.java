package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.ledger.result.LedgerResult;
import com.aionn.payment.domain.model.TransactionLedger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LedgerResultMapper {

    @Mapping(target = "amount", expression = "java(ledger.getAmount().amount())")
    @Mapping(target = "currency", expression = "java(ledger.getAmount().currency())")
    @Mapping(target = "type", expression = "java(ledger.getType().name())")
    LedgerResult toResult(TransactionLedger ledger);
}
