package com.aionn.promotion.application.mapper;

import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.domain.model.UserVoucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserVoucherResultMapper {

    @Mapping(target = "status", expression = "java(userVoucher.getStatus().name())")
    @Mapping(target = "appliedAmount", expression = "java(userVoucher.getAppliedAmount() == null ? null : userVoucher.getAppliedAmount().amount())")
    @Mapping(target = "currency", expression = "java(userVoucher.getAppliedAmount() == null ? null : userVoucher.getAppliedAmount().currency())")
    @Mapping(target = "voucherDiscountAmount", ignore = true)
    @Mapping(target = "voucherCurrency", ignore = true)
    @Mapping(target = "voucherScope", ignore = true)
    @Mapping(target = "voucherValidUntil", ignore = true)
    @Mapping(target = "minOrderValue", ignore = true)
    @Mapping(target = "voucherUsageLimit", ignore = true)
    @Mapping(target = "voucherUsedCount", ignore = true)
    UserVoucherResult toResult(UserVoucher userVoucher);
}
