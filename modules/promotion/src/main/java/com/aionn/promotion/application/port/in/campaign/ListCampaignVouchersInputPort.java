package com.aionn.promotion.application.port.in.campaign;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.domain.valueobject.VoucherType;

import java.util.List;

public interface ListCampaignVouchersInputPort {
    List<VoucherResult> execute(String campaignId, int limit);

    List<VoucherResult> execute(String campaignId, int limit, VoucherType type);

    List<VoucherResult> execute(String campaignId, int limit, VoucherType type, boolean availableOnly);
}
