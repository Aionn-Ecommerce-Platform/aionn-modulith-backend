package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.SettlementLedgerEntry;

import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private MerchantBalancePersistencePort balanceRepo;
    @Mock
    private SettlementLedgerPersistencePort ledgerRepo;
    @Mock
    private OrderQueryPort orderQueryPort;
    @Mock
    private MerchantQueryPort merchantQueryPort;

    private Clock clock;
    private SettlementService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new SettlementService(balanceRepo, ledgerRepo, orderQueryPort, merchantQueryPort, clock);
    }

    @Test
    void onOrderApprovedShouldCreditPendingBalance() {
        OrderQueryPort.OrderSummary summary = new OrderQueryPort.OrderSummary(
                "order-1", "user-1", "merch-1", BigDecimal.valueOf(100), "VND");
        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.of(summary));
        when(merchantQueryPort.findCommissionRate("merch-1")).thenReturn(Optional.of(BigDecimal.valueOf(0.05)));

        MerchantBalance balance = MerchantBalance.empty("merch-1", "VND", clock.instant());
        when(balanceRepo.createIfAbsentAndLock("merch-1", "VND", clock.instant())).thenReturn(balance);

        service.onOrderApproved("order-1", "pay-1");

        verify(balanceRepo).save(any());
        verify(ledgerRepo).save(any());
    }

    @Test
    void onOrderApprovedRoundsCommissionToCurrencyFractionDigits() {
        OrderQueryPort.OrderSummary summary = new OrderQueryPort.OrderSummary(
                "order-1", "user-1", "merch-1", new BigDecimal("101"), "VND");
        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.of(summary));
        when(merchantQueryPort.findCommissionRate("merch-1")).thenReturn(Optional.of(new BigDecimal("0.05")));
        MerchantBalance balance = MerchantBalance.empty("merch-1", "VND", clock.instant());
        when(balanceRepo.createIfAbsentAndLock("merch-1", "VND", clock.instant())).thenReturn(balance);

        service.onOrderApproved("order-1", "pay-1");

        ArgumentCaptor<SettlementLedgerEntry> entry = ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(new BigDecimal("5"), entry.getValue().getCommission());
        assertEquals(new BigDecimal("96"), entry.getValue().getNet());
        assertEquals(new BigDecimal("96"), balance.getPending());
    }

    @Test
    void onOrderApprovedPreservesTwoDecimalCurrencyPrecision() {
        OrderQueryPort.OrderSummary summary = new OrderQueryPort.OrderSummary(
                "order-1", "user-1", "merch-1", new BigDecimal("1.01"), "USD");
        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.of(summary));
        when(merchantQueryPort.findCommissionRate("merch-1")).thenReturn(Optional.of(new BigDecimal("0.05")));
        MerchantBalance balance = MerchantBalance.empty("merch-1", "USD", clock.instant());
        when(balanceRepo.createIfAbsentAndLock("merch-1", "USD", clock.instant())).thenReturn(balance);

        service.onOrderApproved("order-1", "pay-1");

        ArgumentCaptor<SettlementLedgerEntry> entry = ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(new BigDecimal("0.05"), entry.getValue().getCommission());
        assertEquals(new BigDecimal("0.96"), entry.getValue().getNet());
    }

    @Test
    void onOrderApprovedMissingOrderShouldSkip() {
        when(orderQueryPort.findOrderSummary("order-missing")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.onOrderApproved("order-missing", "pay-1"));
        verify(balanceRepo, never()).save(any());
    }

    @Test
    void onOrderCompletedShouldMoveBalanceToAvailable() {
        OrderQueryPort.OrderSummary summary = new OrderQueryPort.OrderSummary(
                "order-1", "user-1", "merch-1", BigDecimal.valueOf(100), "VND");
        SettlementLedgerEntry sale = new SettlementLedgerEntry("sle-1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.SALE, BigDecimal.valueOf(100), BigDecimal.valueOf(5),
                BigDecimal.valueOf(95), BigDecimal.valueOf(95), BigDecimal.ZERO, BigDecimal.ZERO,
                "VND", null, clock.instant());

        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.of(summary));
        when(ledgerRepo.findByOrder("order-1")).thenReturn(java.util.List.of(sale));

        MerchantBalance balance = new MerchantBalance("merch-1", "VND", BigDecimal.valueOf(95), BigDecimal.ZERO, 0L, clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate("merch-1", "VND")).thenReturn(Optional.of(balance));

        service.onOrderCompleted("order-1");

        verify(balanceRepo).save(any());
        verify(ledgerRepo).save(any());
    }

    @Test
    void onPaymentRefundedShouldDeductBalance() {
        SettlementLedgerEntry sale = new SettlementLedgerEntry("sle-1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.SALE, BigDecimal.valueOf(100), BigDecimal.valueOf(5), BigDecimal.valueOf(95), "VND", null, clock.instant());

        when(ledgerRepo.findByOrder("order-1")).thenReturn(java.util.List.of(sale));
        MerchantBalance balance = new MerchantBalance("merch-1", "VND", BigDecimal.ZERO, BigDecimal.valueOf(100), 0L, clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate("merch-1", "VND")).thenReturn(Optional.of(balance));

        service.onPaymentRefunded("order-1", "pay-1", "refund-1", BigDecimal.valueOf(100), "VND");

        verify(balanceRepo).save(any());
        verify(ledgerRepo).save(any());
    }

    @Test
    void completedOrderDoesNotMoveSaleAmountUsedToRepayReceivable() {
        OrderQueryPort.OrderSummary summary = new OrderQueryPort.OrderSummary(
                "order-1", "user-1", "merch-1", BigDecimal.valueOf(100), "VND");
        SettlementLedgerEntry sale = new SettlementLedgerEntry(
                "sle-1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.SALE,
                BigDecimal.valueOf(100), BigDecimal.valueOf(5), BigDecimal.valueOf(95),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(-95),
                "VND", null, clock.instant());
        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.of(summary));
        when(ledgerRepo.findByOrder("order-1")).thenReturn(java.util.List.of(sale));
        MerchantBalance balance = MerchantBalance.empty("merch-1", "VND", clock.instant());
        when(balanceRepo.lockForUpdate("merch-1", "VND")).thenReturn(Optional.of(balance));

        service.onOrderCompleted("order-1");

        verify(balanceRepo, never()).save(any());
        ArgumentCaptor<SettlementLedgerEntry> entry = ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(BigDecimal.ZERO, entry.getValue().getPendingDelta());
        assertEquals(BigDecimal.ZERO, entry.getValue().getAvailableDelta());
    }

    @Test
    void partialRefundsAllocateRoundingFromCumulativeTotal() {
        SettlementLedgerEntry sale = new SettlementLedgerEntry("sle-1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.SALE, BigDecimal.valueOf(100), BigDecimal.valueOf(5),
                BigDecimal.valueOf(95), "VND", null, clock.instant());
        SettlementLedgerEntry firstRefund = new SettlementLedgerEntry(
                "sle-r1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.REFUND, BigDecimal.valueOf(50), BigDecimal.ZERO,
                BigDecimal.valueOf(-48), "VND", null, clock.instant());
        when(ledgerRepo.findByOrder("order-1"))
                .thenReturn(java.util.List.of(sale))
                .thenReturn(java.util.List.of(sale, firstRefund));
        MerchantBalance balance = new MerchantBalance(
                "merch-1", "VND", BigDecimal.ZERO, BigDecimal.valueOf(47), 0L,
                clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate("merch-1", "VND")).thenReturn(Optional.of(balance));

        service.onPaymentRefunded("order-1", "pay-1", "refund-2", BigDecimal.valueOf(50), "VND");

        ArgumentCaptor<SettlementLedgerEntry> entry = ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(BigDecimal.valueOf(-47), entry.getValue().getNet());
        assertEquals(BigDecimal.ZERO, balance.getAvailable());
    }

    @Test
    void refundUsesAvailableAndPendingTogether() {
        SettlementLedgerEntry sale = new SettlementLedgerEntry("sle-1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.SALE, BigDecimal.valueOf(100), BigDecimal.valueOf(5),
                BigDecimal.valueOf(95), "VND", null, clock.instant());
        when(ledgerRepo.findByOrder("order-1")).thenReturn(java.util.List.of(sale));
        MerchantBalance balance = new MerchantBalance(
                "merch-1", "VND", BigDecimal.valueOf(55), BigDecimal.valueOf(40), 0L,
                clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate("merch-1", "VND")).thenReturn(Optional.of(balance));

        service.onPaymentRefunded("order-1", "pay-1", "refund-split",
                BigDecimal.valueOf(100), "VND");

        assertEquals(BigDecimal.ZERO, balance.getAvailable());
        assertEquals(BigDecimal.ZERO, balance.getPending());
        verify(balanceRepo).save(balance);
        verify(ledgerRepo).save(any());
    }

    @Test
    void insufficientRefundBalanceCreatesMerchantReceivable() {
        SettlementLedgerEntry sale = new SettlementLedgerEntry("sle-1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.SALE, BigDecimal.valueOf(100), BigDecimal.valueOf(5),
                BigDecimal.valueOf(95), "VND", null, clock.instant());
        when(ledgerRepo.findByOrder("order-1")).thenReturn(java.util.List.of(sale));
        MerchantBalance balance = new MerchantBalance(
                "merch-1", "VND", BigDecimal.TEN, BigDecimal.TEN, 0L,
                clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate("merch-1", "VND")).thenReturn(Optional.of(balance));

        service.onPaymentRefunded(
                "order-1", "pay-1", "refund-insufficient", BigDecimal.valueOf(100), "VND");

        assertEquals(BigDecimal.ZERO, balance.getPending());
        assertEquals(BigDecimal.ZERO, balance.getAvailable());
        assertEquals(BigDecimal.valueOf(75), balance.getReceivable());
        ArgumentCaptor<SettlementLedgerEntry> entry = ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(BigDecimal.valueOf(-10), entry.getValue().getPendingDelta());
        assertEquals(BigDecimal.valueOf(-10), entry.getValue().getAvailableDelta());
        assertEquals(BigDecimal.valueOf(75), entry.getValue().getReceivableDelta());
    }

    @Test
    void newSaleRepaysReceivableBeforeCreditingPending() {
        OrderQueryPort.OrderSummary summary = new OrderQueryPort.OrderSummary(
                "order-2", "user-1", "merch-1", BigDecimal.valueOf(100), "VND");
        when(orderQueryPort.findOrderSummary("order-2")).thenReturn(Optional.of(summary));
        when(merchantQueryPort.findCommissionRate("merch-1")).thenReturn(Optional.of(BigDecimal.valueOf(0.05)));
        MerchantBalance balance = new MerchantBalance(
                "merch-1", "VND", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(75),
                0L, clock.instant(), clock.instant());
        when(balanceRepo.createIfAbsentAndLock("merch-1", "VND", clock.instant())).thenReturn(balance);

        service.onOrderApproved("order-2", "pay-2");

        assertEquals(BigDecimal.ZERO, balance.getReceivable());
        assertEquals(BigDecimal.valueOf(20), balance.getPending());
        ArgumentCaptor<SettlementLedgerEntry> entry = ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(BigDecimal.valueOf(20), entry.getValue().getPendingDelta());
        assertEquals(BigDecimal.valueOf(-75), entry.getValue().getReceivableDelta());
    }

    @Test
    void duplicateRefundEffectDoesNotMutateBalance() {
        when(ledgerRepo.existsById(anyString())).thenReturn(true);

        service.onPaymentRefunded("order-1", "pay-1", "refund-duplicate",
                BigDecimal.valueOf(100), "VND");

        verify(balanceRepo, never()).lockForUpdate(anyString(), anyString());
        verify(ledgerRepo, never()).save(any());
    }
}
