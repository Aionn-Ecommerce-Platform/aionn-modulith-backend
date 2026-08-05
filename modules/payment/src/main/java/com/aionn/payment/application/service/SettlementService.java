package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
import com.aionn.payment.domain.model.SettlementLedgerEntry.SettlementKind;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import com.aionn.sharedkernel.util.Sha256Hasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.0500");

    private final MerchantBalancePersistencePort balanceRepo;
    private final SettlementLedgerPersistencePort ledgerRepo;
    private final OrderQueryPort orderQueryPort;
    private final MerchantQueryPort merchantQueryPort;
    private final Clock clock;

    public void onOrderApproved(String orderId, String paymentId) {
        String effectId = effectId("SALE", orderId);
        if (ledgerRepo.existsById(effectId)) return;
        OrderQueryPort.OrderSummary order = orderQueryPort.findOrderSummary(orderId).orElse(null);
        if (order == null) {
            log.warn("Settlement: order {} not found, skipping SALE entry", orderId);
            return;
        }
        BigDecimal rate = merchantQueryPort.findCommissionRate(order.merchantId()).orElse(DEFAULT_RATE);
        BigDecimal commission = currencyAmount(order.totalAmount().multiply(rate), order.currency());
        BigDecimal net = currencyAmount(order.totalAmount().subtract(commission), order.currency());

        Instant now = clock.instant();
        MerchantBalance balance = loadOrCreate(order.merchantId(), order.currency(), now);
        if (ledgerRepo.existsById(effectId)) return;
        balance.addPending(net, now);
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                effectId,
                order.merchantId(), orderId, paymentId, null,
                SettlementKind.SALE, order.totalAmount(), commission, net,
                order.currency(), null, now));
    }

    public void onOrderCompleted(String orderId) {
        String effectId = effectId("MOVE", orderId);
        if (ledgerRepo.existsById(effectId)) return;
        OrderQueryPort.OrderSummary order = orderQueryPort.findOrderSummary(orderId).orElse(null);
        if (order == null) return;
        SettlementLedgerEntry sale = findSaleEntry(orderId);
        if (sale == null) return;

        Instant now = clock.instant();
        MerchantBalance balance = balanceRepo.lockForUpdate(order.merchantId(), order.currency())
                .orElse(null);
        if (balance == null) {
            log.warn("Settlement: balance missing for completed order {}", orderId);
            return;
        }
        if (ledgerRepo.existsById(effectId)) return;
        balance.moveToAvailable(sale.getNet(), now);
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                effectId,
                order.merchantId(), orderId, sale.getPaymentId(), null,
                SettlementKind.MOVE_AVAILABLE, sale.getNet(), BigDecimal.ZERO, sale.getNet(),
                order.currency(), null, now));
    }

    public void onPaymentRefunded(String orderId, String paymentId, String refundId,
            BigDecimal refundAmount, String currency) {
        String effectId = effectId("REFUND", refundId);
        if (ledgerRepo.existsById(effectId)) return;
        SettlementLedgerEntry sale = findSaleEntry(orderId);
        if (sale == null) return;
        MerchantBalance balance = balanceRepo.lockForUpdate(sale.getMerchantId(), currency).orElse(null);
        if (balance == null) return;
        if (ledgerRepo.existsById(effectId)) return;

        // Re-read under the merchant-balance lock so concurrent partial refunds allocate
        // against the same cumulative ledger state. Rounding the cumulative target and
        // subtracting what was already allocated guarantees that all parts add up to
        // exactly the sale net amount.
        java.util.List<SettlementLedgerEntry> entries = ledgerRepo.findByOrder(orderId);
        sale = entries.stream()
                .filter(entry -> entry.getKind() == SettlementKind.SALE)
                .findFirst()
                .orElse(null);
        if (sale == null || refundAmount == null || refundAmount.signum() <= 0
                || !sale.getCurrency().equals(currency)) {
            return;
        }
        BigDecimal previouslyRefundedGross = entries.stream()
                .filter(entry -> entry.getKind() == SettlementKind.REFUND)
                .map(SettlementLedgerEntry::getGross)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previouslyAllocatedNet = entries.stream()
                .filter(entry -> entry.getKind() == SettlementKind.REFUND)
                .map(SettlementLedgerEntry::getNet)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cumulativeRefund = previouslyRefundedGross.add(refundAmount).min(sale.getGross());
        BigDecimal cumulativeProportion = cumulativeRefund.divide(sale.getGross(), 8, RoundingMode.HALF_UP);
        BigDecimal cumulativeNet = currencyAmount(sale.getNet().multiply(cumulativeProportion), currency);
        BigDecimal netDeduct = cumulativeNet.subtract(previouslyAllocatedNet);
        if (netDeduct.signum() <= 0) return;

        Instant now = clock.instant();
        BigDecimal totalBalance = balance.getAvailable().add(balance.getPending());
        if (totalBalance.compareTo(netDeduct) < 0) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_EXCEEDED,
                    "Settlement balance cannot cover refund for order " + orderId);
        }
        BigDecimal availableDeduct = balance.getAvailable().min(netDeduct);
        if (availableDeduct.signum() > 0) {
            balance.debitAvailable(availableDeduct, now);
        }
        BigDecimal pendingDeduct = netDeduct.subtract(availableDeduct);
        if (pendingDeduct.signum() > 0) {
            balance.reversePending(pendingDeduct, now);
        }
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                effectId,
                sale.getMerchantId(), orderId, paymentId, null,
                SettlementKind.REFUND, refundAmount, BigDecimal.ZERO, netDeduct.negate(),
                currency, "payment refunded", now));
    }

    private MerchantBalance loadOrCreate(String merchantId, String currency, Instant now) {
        return balanceRepo.createIfAbsentAndLock(merchantId, currency, now);
    }

    private SettlementLedgerEntry findSaleEntry(String orderId) {
        return ledgerRepo.findByOrder(orderId).stream()
                .filter(e -> e.getKind() == SettlementKind.SALE)
                .findFirst()
                .orElse(null);
    }

    private static BigDecimal currencyAmount(BigDecimal amount, String currency) {
        return Money.of(amount, currency).amount();
    }

    private static String effectId(String kind, String businessKey) {
        return "SLE_" + kind + "_" + Sha256Hasher.hexDigest(businessKey).substring(0, 32);
    }
}
