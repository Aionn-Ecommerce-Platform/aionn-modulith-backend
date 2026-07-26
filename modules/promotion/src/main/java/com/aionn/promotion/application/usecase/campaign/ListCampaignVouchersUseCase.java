package com.aionn.promotion.application.usecase.campaign;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.mapper.VoucherResultMapper;
import com.aionn.promotion.application.port.in.campaign.ListCampaignVouchersInputPort;
import com.aionn.promotion.application.service.PromotionCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCampaignVouchersUseCase implements ListCampaignVouchersInputPort {

    private final PromotionCampaignService promotionCampaignService;
    private final VoucherResultMapper voucherResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResult> execute(String campaignId, int limit) {
        return voucherResultMapper.toResults(
                promotionCampaignService.listVouchersByCampaignId(campaignId, limit));
    }
}
