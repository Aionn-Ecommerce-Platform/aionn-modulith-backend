package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.MerchantPayoutPersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private MerchantBalancePersistencePort balanceRepo;
    @Mock
    private MerchantPayoutPersistencePort payoutRepo;
    @Mock
    private SettlementLedgerPersistencePort ledgerRepo;
    @Mock
    private MerchantQueryPort merchantQueryPort;

    private Clock clock;
    private PayoutService payoutService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        payoutService = new PayoutService(balanceRepo, payoutRepo, ledgerRepo, merchantQueryPort, clock);
    }

    @Test
    void shouldRequestPayoutSuccessfully() {
        String ownerId = "owner-1";
        String merchantId = "m-1";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "VND";

        when(merchantQueryPort.findMerchantIdByOwnerId(ownerId)).thenReturn(Optional.of(merchantId));
        
        MerchantBalance balance = new MerchantBalance(merchantId, currency, BigDecimal.ZERO, new BigDecimal("200.00"), 0L, clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate(merchantId, currency)).thenReturn(Optional.of(balance));
        when(payoutRepo.save(any(MerchantPayout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MerchantPayoutResult result = payoutService.requestPayout(ownerId, amount, currency, "VCB", "123", "Account Name", "Request payout");

        assertNotNull(result);
        assertEquals(merchantId, result.merchantId());
        assertEquals(amount, result.amount());
        assertEquals("PENDING", result.status());
        
        verify(balanceRepo).save(balance);
        verify(ledgerRepo).save(any());
    }

    @Test
    void shouldFailRequestPayoutWhenNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-invalid")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> 
            payoutService.requestPayout("owner-invalid", new BigDecimal("10.00"), "VND", "VCB", "123", "Name", "Note")
        );
    }

    @Test
    void shouldFailRequestPayoutWhenBalanceNotFound() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("m-1"));
        when(balanceRepo.lockForUpdate("m-1", "VND")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> 
            payoutService.requestPayout("owner-1", new BigDecimal("10.00"), "VND", "VCB", "123", "Name", "Note")
        );
    }

    @Test
    void shouldMarkCompleted() {
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING, "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findById("p-1")).thenReturn(Optional.of(payout));
        when(payoutRepo.save(payout)).thenReturn(payout);

        MerchantPayoutResult result = payoutService.markCompleted("p-1", "external-123");

        assertEquals("COMPLETED", result.status());
        assertEquals("external-123", result.externalRef());
        assertNotNull(result.completedAt());
    }

    @Test
    void shouldMarkFailedAndReverseBalance() {
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING, "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findById("p-1")).thenReturn(Optional.of(payout));
        when(payoutRepo.save(payout)).thenReturn(payout);

        MerchantBalance balance = new MerchantBalance("m-1", "VND", BigDecimal.ZERO, BigDecimal.ZERO, 0L, clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate("m-1", "VND")).thenReturn(Optional.of(balance));

        MerchantPayoutResult result = payoutService.markFailed("p-1", "Bank rejected");

        assertEquals("FAILED", result.status());
        assertEquals("Bank rejected", result.failureReason());
        assertEquals(new BigDecimal("100.00"), balance.getAvailable());
        verify(ledgerRepo).save(any());
    }

    @Test
    void shouldGetBalanceForOwner() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("m-1"));
        MerchantBalance balance = new MerchantBalance("m-1", "VND", BigDecimal.ZERO, new BigDecimal("500.00"), 0L, clock.instant(), clock.instant());
        when(balanceRepo.find("m-1", "VND")).thenReturn(Optional.of(balance));

        MerchantBalanceResult result = payoutService.getBalanceForOwner("owner-1", "VND");

        assertEquals("m-1", result.merchantId());
        assertEquals(new BigDecimal("500.00"), result.available());
    }

    @Test
    void shouldListForOwner() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("m-1"));
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING, "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findByMerchant("m-1", 10)).thenReturn(List.of(payout));

        List<MerchantPayoutResult> results = payoutService.listForOwner("owner-1", 10);

        assertEquals(1, results.size());
        assertEquals("p-1", results.get(0).payoutId());
    }

    @Test
    void shouldListByStatus() {
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING, "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findByStatus(PayoutStatus.PENDING, 5)).thenReturn(List.of(payout));

        List<MerchantPayoutResult> results = payoutService.listByStatus(PayoutStatus.PENDING, 5);

        assertEquals(1, results.size());
        assertEquals("PENDING", results.get(0).status());
    }
}
