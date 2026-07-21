package com.aionn.payment.domain.model;

import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MerchantPayoutTest {

    @Test
    void shouldRequestPayoutCorrectly() {
        Instant now = Instant.now();
        MerchantPayout payout = MerchantPayout.request("pay-1", "m-1", new BigDecimal("1000.00"), "VND", "VCB", "123", "Account Name", "Test Payout", now);

        assertEquals("pay-1", payout.getPayoutId());
        assertEquals("m-1", payout.getMerchantId());
        assertEquals(new BigDecimal("1000.00"), payout.getAmount());
        assertEquals("VND", payout.getCurrency());
        assertEquals(PayoutStatus.PENDING, payout.getStatus());
        assertEquals("VCB", payout.getBankName());
        assertEquals("123", payout.getBankAccountNo());
        assertEquals("Account Name", payout.getBankAccountName());
        assertEquals("Test Payout", payout.getNote());
        assertEquals(now, payout.getRequestedAt());
    }

    @Test
    void shouldFailToRequestPayoutWithNonPositiveAmount() {
        Instant now = Instant.now();
        assertThrows(PaymentException.class, () -> 
            MerchantPayout.request("pay-1", "m-1", new BigDecimal("-10.00"), "VND", "VCB", "123", "Account Name", "Test", now)
        );
        assertThrows(PaymentException.class, () -> 
            MerchantPayout.request("pay-1", "m-1", BigDecimal.ZERO, "VND", "VCB", "123", "Account Name", "Test", now)
        );
    }

    @Test
    void shouldTransitionToProcessing() {
        Instant now = Instant.now();
        MerchantPayout payout = MerchantPayout.request("pay-1", "m-1", new BigDecimal("100.00"), "VND", "VCB", "123", "A Name", null, now);
        
        payout.markProcessing();
        assertEquals(PayoutStatus.PROCESSING, payout.getStatus());
    }

    @Test
    void shouldTransitionToCompleted() {
        Instant now = Instant.now();
        MerchantPayout payout = MerchantPayout.request("pay-1", "m-1", new BigDecimal("100.00"), "VND", "VCB", "123", "A Name", null, now);
        
        payout.markProcessing();
        payout.markCompleted("ref-123", now.plusSeconds(10));
        
        assertEquals(PayoutStatus.COMPLETED, payout.getStatus());
        assertEquals("ref-123", payout.getExternalRef());
        assertEquals(now.plusSeconds(10), payout.getCompletedAt());
    }

    @Test
    void shouldTransitionToFailed() {
        Instant now = Instant.now();
        MerchantPayout payout = MerchantPayout.request("pay-1", "m-1", new BigDecimal("100.00"), "VND", "VCB", "123", "A Name", null, now);
        
        payout.markFailed("Insufficient bank funds", now.plusSeconds(10));
        
        assertEquals(PayoutStatus.FAILED, payout.getStatus());
        assertEquals("Insufficient bank funds", payout.getFailureReason());
        assertEquals(now.plusSeconds(10), payout.getFailedAt());
    }

    @Test
    void shouldFailToChangeStatusWhenAlreadyInFinalState() {
        Instant now = Instant.now();
        MerchantPayout payout = MerchantPayout.request("pay-1", "m-1", new BigDecimal("100.00"), "VND", "VCB", "123", "A Name", null, now);
        
        payout.markFailed("Failed", now);
        
        assertThrows(PaymentException.class, payout::markProcessing);
        assertThrows(PaymentException.class, () -> payout.markCompleted("ref", now));
        assertThrows(PaymentException.class, () -> payout.markFailed("Reason", now));
    }
}
