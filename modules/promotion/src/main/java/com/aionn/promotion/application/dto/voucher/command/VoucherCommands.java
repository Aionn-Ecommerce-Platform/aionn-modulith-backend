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

        public record GrantVoucher(String userId, String voucherCode, String reason, String entitlementKey)
                implements Command {

            /** Backwards-compatible grant without a per-delivery dedup key. */
            public GrantVoucher(String userId, String voucherCode, String reason) {
                this(userId, voucherCode, reason, null);
            }
        }

        public record IssueShopVoucher(
                        String ownerId,
                        String voucherCode,
                        BigDecimal discountAmount,
                        String currency,
                        int usageLimit,
                        Instant validFrom,
                        Instant validUntil) implements Command {
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
