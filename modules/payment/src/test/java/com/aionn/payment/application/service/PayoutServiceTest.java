package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.MerchantPayoutPersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
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

        MerchantBalance balance = new MerchantBalance(merchantId, currency, BigDecimal.ZERO, new BigDecimal("200.00"),
                0L, clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate(merchantId, currency)).thenReturn(Optional.of(balance));
        when(payoutRepo.save(any(MerchantPayout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MerchantPayout result = payoutService.requestPayout(ownerId, amount, currency, "VCB", "123",
                "Account Name", "Request payout");

        assertNotNull(result);
        assertEquals(merchantId, result.getMerchantId());
        assertEquals(amount, result.getAmount());
        assertEquals(PayoutStatus.PENDING, result.getStatus());

        verify(balanceRepo).save(balance);
        org.mockito.ArgumentCaptor<SettlementLedgerEntry> entry =
                org.mockito.ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(amount.negate(), entry.getValue().getAvailableDelta());
    }

    @Test
    void shouldFailRequestPayoutWhenNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-invalid")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> payoutService.requestPayout("owner-invalid", new BigDecimal("10.00"),
                "VND", "VCB", "123", "Name", "Note"));
    }

    @Test
    void shouldFailRequestPayoutWhenBalanceNotFound() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("m-1"));
        when(balanceRepo.lockForUpdate("m-1", "VND")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> payoutService.requestPayout("owner-1", new BigDecimal("10.00"),
                "VND", "VCB", "123", "Name", "Note"));
    }

    @Test
    void shouldMarkCompleted() {
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING,
                "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findById("p-1")).thenReturn(Optional.of(payout));
        when(payoutRepo.save(payout)).thenReturn(payout);

        MerchantPayout result = payoutService.markCompleted("p-1", "external-123");

        assertEquals(PayoutStatus.COMPLETED, result.getStatus());
        assertEquals("external-123", result.getExternalRef());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void shouldMarkFailedAndReverseBalance() {
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING,
                "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findById("p-1")).thenReturn(Optional.of(payout));
        when(payoutRepo.save(payout)).thenReturn(payout);

        MerchantBalance balance = new MerchantBalance("m-1", "VND", BigDecimal.ZERO, BigDecimal.ZERO, 0L,
                clock.instant(), clock.instant());
        when(balanceRepo.lockForUpdate("m-1", "VND")).thenReturn(Optional.of(balance));

        MerchantPayout result = payoutService.markFailed("p-1", "Bank rejected");

        assertEquals(PayoutStatus.FAILED, result.getStatus());
        assertEquals("Bank rejected", result.getFailureReason());
        assertEquals(new BigDecimal("100.00"), balance.getAvailable());
        org.mockito.ArgumentCaptor<SettlementLedgerEntry> entry =
                org.mockito.ArgumentCaptor.forClass(SettlementLedgerEntry.class);
        verify(ledgerRepo).save(entry.capture());
        assertEquals(new BigDecimal("100.00"), entry.getValue().getAvailableDelta());
    }

    @Test
    void shouldGetBalanceForOwner() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("m-1"));
        MerchantBalance balance = new MerchantBalance("m-1", "VND", BigDecimal.ZERO, new BigDecimal("500.00"), 0L,
                clock.instant(), clock.instant());
        when(balanceRepo.find("m-1", "VND")).thenReturn(Optional.of(balance));

        MerchantBalance result = payoutService.getBalanceForOwner("owner-1", "VND");

        assertEquals("m-1", result.getMerchantId());
        assertEquals(new BigDecimal("500.00"), result.getAvailable());
    }

    @Test
    void shouldListForOwner() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("m-1"));
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING,
                "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findByMerchant("m-1", 10)).thenReturn(List.of(payout));

        List<MerchantPayout> results = payoutService.listForOwner("owner-1", 10);

        assertEquals(1, results.size());
        assertEquals("p-1", results.get(0).getPayoutId());
    }

    @Test
    void shouldListByStatus() {
        MerchantPayout payout = new MerchantPayout("p-1", "m-1", new BigDecimal("100.00"), "VND", PayoutStatus.PENDING,
                "VCB", "123", "Name", null, null, clock.instant(), null, null, null, 0L);
        when(payoutRepo.findByStatus(PayoutStatus.PENDING, 5)).thenReturn(List.of(payout));

        List<MerchantPayout> results = payoutService.listByStatus(PayoutStatus.PENDING, 5);

        assertEquals(1, results.size());
        assertEquals(PayoutStatus.PENDING, results.get(0).getStatus());
    }
}
