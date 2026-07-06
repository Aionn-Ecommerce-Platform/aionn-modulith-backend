package com.aionn.catalog.application.dto.merchant.command;

import com.aionn.sharedkernel.application.command.Command;
import java.math.BigDecimal;

public record UpdateMerchantCommissionRateCommand(String merchantId, BigDecimal commissionRate) implements Command {
}
