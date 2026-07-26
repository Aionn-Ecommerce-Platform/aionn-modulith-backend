package com.aionn.promotion.adapter.rest.mapper.voucher;

import com.aionn.promotion.adapter.rest.dto.voucher.ApplyVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.IssueVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.ReleaseVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.ReserveVoucherRequest;
import com.aionn.promotion.adapter.rest.dto.voucher.response.UserVoucherResponse;
import com.aionn.promotion.adapter.rest.dto.voucher.response.VoucherResponse;
import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VoucherDtoMapper {

    @Mapping(target = "scope", expression = "java(result.scope() == null ? null : result.scope().name())")
    VoucherResponse toResponse(VoucherResult result);

    List<VoucherResponse> toResponses(List<VoucherResult> results);

    UserVoucherResponse toResponse(UserVoucherResult result);

    List<UserVoucherResponse> toUserVoucherResponses(List<UserVoucherResult> results);

    default VoucherCommands.IssueShopVoucher toIssueShopVoucherCommand(String ownerId,
            IssueVoucherRequest request) {
        return new VoucherCommands.IssueShopVoucher(ownerId, request.voucherCode(),
                request.discountAmount(), request.currency(), request.usageLimit(),
                request.validFrom(), request.validUntil());
    }

    default VoucherCommands.ClaimVoucher toClaimCommand(String userId, String voucherCode) {
        return new VoucherCommands.ClaimVoucher(userId, voucherCode);
    }

    default VoucherCommands.ReserveVoucher toReserveCommand(String userId, String voucherCode,
            ReserveVoucherRequest request) {
        return new VoucherCommands.ReserveVoucher(userId, voucherCode, request.orderId(),
                request.orderValue(), request.currency(), request.orderCategoryIds(),
                request.expiresAt());
    }

    default VoucherCommands.ApplyVoucher toApplyCommand(String userId, String voucherCode,
            ApplyVoucherRequest request) {
        return new VoucherCommands.ApplyVoucher(userId, voucherCode, request.orderId(),
                request.appliedAmount(), request.currency());
    }

    default VoucherCommands.ReleaseVoucher toReleaseCommand(String userId, String voucherCode,
            ReleaseVoucherRequest request) {
        return new VoucherCommands.ReleaseVoucher(userId, voucherCode, request.orderId(),
                request.reason());
    }
}
