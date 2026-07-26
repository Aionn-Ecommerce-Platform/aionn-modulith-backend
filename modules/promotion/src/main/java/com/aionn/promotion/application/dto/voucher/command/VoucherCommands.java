package com.aionn.promotion.application.dto.voucher.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class VoucherCommands {

        private VoucherCommands() {
        }

        public record ClaimVoucher(String userId, String voucherCode) implements Command {
        }

        public record ReserveVoucher(
                        String userId,
                        String voucherCode,
                        String orderId,
                        BigDecimal orderValue,
                        String currency,
                        List<String> orderCategoryIds,
                        Instant expiresAt) implements Command {
        }

        public record ApplyVoucher(
                        String userId,
                        String voucherCode,
                        String orderId,
                        BigDecimal appliedAmount,
                        String currency) implements Command {
        }

        public record ReleaseVoucher(
                        String userId,
                        String voucherCode,
                        String orderId,
                        String reason) implements Command {
        }
}
