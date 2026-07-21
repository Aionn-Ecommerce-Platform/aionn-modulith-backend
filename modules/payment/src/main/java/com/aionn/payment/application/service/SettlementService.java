package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
import com.aionn.payment.domain.model.SettlementLedgerEntry.SettlementKind;
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
        BigDecimal commission = order.totalAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = order.totalAmount().subtract(commission);

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
        BigDecimal proportion = refundAmount.divide(sale.getGross(), 8, RoundingMode.HALF_UP);
        BigDecimal netDeduct = sale.getNet().multiply(proportion).setScale(2, RoundingMode.HALF_UP);

        Instant now = clock.instant();
        MerchantBalance balance = balanceRepo.lockForUpdate(sale.getMerchantId(), currency).orElse(null);
        if (balance == null) return;
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
        return balanceRepo.lockForUpdate(merchantId, currency)
                .orElseGet(() -> MerchantBalance.empty(merchantId, currency, now));
    }

    private SettlementLedgerEntry findSaleEntry(String orderId) {
        return ledgerRepo.findByOrder(orderId).stream()
                .filter(e -> e.getKind() == SettlementKind.SALE)
                .findFirst()
                .orElse(null);
    }
}
