package com.aionn.promotion.application.usecase.campaign;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.mapper.VoucherResultMapper;
import com.aionn.promotion.application.port.in.campaign.IssueCampaignVoucherInputPort;
import com.aionn.promotion.application.service.PromotionCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueCampaignVoucherUseCase implements IssueCampaignVoucherInputPort {

    private final PromotionCampaignService promotionCampaignService;
    private final VoucherResultMapper voucherResultMapper;

    @Override
    @Transactional
    public VoucherResult execute(CampaignCommands.IssueVoucher command) {
        return voucherResultMapper.toResult(promotionCampaignService.issueVoucher(command));
    }
}
