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
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        OrderQueryPort.OrderSummary summary = new OrderQueryPort.OrderSummary("order-1", "merch-1", BigDecimal.valueOf(100), "VND");
        when(orderQueryPort.findOrderSummary("order-1")).thenReturn(Optional.of(summary));
        when(merchantQueryPort.findCommissionRate("merch-1")).thenReturn(Optional.of(BigDecimal.valueOf(0.05)));

        MerchantBalance balance = MerchantBalance.empty("merch-1", "VND", clock.instant());
        when(balanceRepo.lockForUpdate("merch-1", "VND")).thenReturn(Optional.of(balance));

        service.onOrderApproved("order-1", "pay-1");

        verify(balanceRepo).save(any());
        verify(ledgerRepo).save(any());
    }

    @Test
    void onOrderApprovedMissingOrderShouldSkip() {
        when(orderQueryPort.findOrderSummary("order-missing")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.onOrderApproved("order-missing", "pay-1"));
        verify(balanceRepo, never()).save(any());
    }
}
