package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
import com.aionn.payment.domain.model.SettlementLedgerEntry.SettlementKind;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
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
        balance.addPending(net, now);
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
                order.merchantId(), orderId, paymentId, null,
                SettlementKind.SALE, order.totalAmount(), commission, net,
                order.currency(), null, now));
    }

    public void onOrderCompleted(String orderId) {
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
        balance.moveToAvailable(sale.getNet(), now);
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
                order.merchantId(), orderId, sale.getPaymentId(), null,
                SettlementKind.MOVE_AVAILABLE, sale.getNet(), BigDecimal.ZERO, sale.getNet(),
                order.currency(), null, now));
    }

    public void onOrderCancelled(String orderId) {
        SettlementLedgerEntry sale = findSaleEntry(orderId);
        if (sale == null) return;
        boolean alreadyMoved = ledgerRepo.findByOrder(orderId).stream()
                .anyMatch(e -> e.getKind() == SettlementKind.MOVE_AVAILABLE);

        Instant now = clock.instant();
        MerchantBalance balance = balanceRepo.lockForUpdate(sale.getMerchantId(), sale.getCurrency())
                .orElse(null);
        if (balance == null) return;

        if (alreadyMoved) {
            balance.debitAvailable(sale.getNet(), now);
        } else {
            balance.reversePending(sale.getNet(), now);
        }
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
                sale.getMerchantId(), orderId, sale.getPaymentId(), null,
                SettlementKind.REVERSAL, sale.getNet(), BigDecimal.ZERO, sale.getNet().negate(),
                sale.getCurrency(), "order cancelled", now));
    }

    public void onPaymentRefunded(String orderId, String paymentId, BigDecimal refundAmount, String currency) {
        SettlementLedgerEntry sale = findSaleEntry(orderId);
        if (sale == null) return;
        MerchantBalance balance = balanceRepo.lockForUpdate(sale.getMerchantId(), currency).orElse(null);
        if (balance == null) return;

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
        if (balance.getAvailable().compareTo(netDeduct) >= 0) {
            balance.debitAvailable(netDeduct, now);
        } else if (balance.getPending().compareTo(netDeduct) >= 0) {
            balance.reversePending(netDeduct, now);
        } else {
            log.warn("Settlement: insufficient balance for refund of order {}", orderId);
            return;
        }
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
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
}
