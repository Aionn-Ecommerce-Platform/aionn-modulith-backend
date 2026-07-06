package com.aionn.catalog.application.dto.merchant.command;

import com.aionn.sharedkernel.application.command.Command;
import java.math.BigDecimal;

public record UpdateGlobalCommissionRateCommand(BigDecimal defaultCommissionRate) implements Command {
}
