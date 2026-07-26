package com.aionn.promotion.application.port.in.campaign;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;

import java.util.List;

public interface ListCampaignVouchersInputPort {
    List<VoucherResult> execute(String campaignId, int limit);
}
