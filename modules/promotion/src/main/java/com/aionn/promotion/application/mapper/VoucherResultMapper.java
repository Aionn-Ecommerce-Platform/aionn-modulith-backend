package com.aionn.promotion.application.mapper;

import java.util.List;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.domain.model.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherResultMapper {

    @Mapping(target = "discountAmount", expression = "java(voucher.getDiscountAmount().amount())")
    @Mapping(target = "currency", expression = "java(voucher.getDiscountAmount().currency())")
    VoucherResult toResult(Voucher voucher);

    List<VoucherResult> toResults(List<Voucher> vouchers);
}
