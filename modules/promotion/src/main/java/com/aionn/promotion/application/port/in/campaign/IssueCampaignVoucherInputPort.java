package com.aionn.promotion.application.port.in.campaign;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;

public interface IssueCampaignVoucherInputPort {
    VoucherResult execute(CampaignCommands.IssueVoucher command);
}
