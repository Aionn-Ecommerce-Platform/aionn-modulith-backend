package com.aionn.payment.domain.model;

import com.aionn.payment.domain.exception.PaymentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MerchantBalanceTest {

    @Test
    void shouldCreateEmptyBalance() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);

        assertEquals("m-1", balance.getMerchantId());
        assertEquals("VND", balance.getCurrency());
        assertEquals(BigDecimal.ZERO, balance.getPending());
        assertEquals(BigDecimal.ZERO, balance.getAvailable());
        assertEquals(now, balance.getCreatedAt());
        assertEquals(now, balance.getUpdatedAt());
    }

    @Test
    void shouldAddPendingCorrectly() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        balance.addPending(new BigDecimal("100.00"), now.plusSeconds(1));
        assertEquals(new BigDecimal("100.00"), balance.getPending());
        assertEquals(now.plusSeconds(1), balance.getUpdatedAt());
    }

    @Test
    void shouldFailToAddNegativeOrZeroPending() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        assertThrows(PaymentException.class, () -> balance.addPending(new BigDecimal("-10.00"), now));
        assertThrows(PaymentException.class, () -> balance.addPending(BigDecimal.ZERO, now));
    }

    @Test
    void shouldMoveToAvailableCorrectly() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        balance.addPending(new BigDecimal("150.00"), now);
        balance.moveToAvailable(new BigDecimal("100.00"), now.plusSeconds(2));
        
        assertEquals(new BigDecimal("50.00"), balance.getPending());
        assertEquals(new BigDecimal("100.00"), balance.getAvailable());
    }

    @Test
    void shouldFailToMoveMoreThanPending() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        balance.addPending(new BigDecimal("50.00"), now);
        assertThrows(PaymentException.class, () -> balance.moveToAvailable(new BigDecimal("60.00"), now));
    }

    @Test
    void shouldReversePendingCorrectly() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        balance.addPending(new BigDecimal("100.00"), now);
        balance.reversePending(new BigDecimal("40.00"), now.plusSeconds(1));
        
        assertEquals(new BigDecimal("60.00"), balance.getPending());
    }

    @Test
    void shouldDebitAvailableCorrectly() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        balance.addPending(new BigDecimal("100.00"), now);
        balance.moveToAvailable(new BigDecimal("100.00"), now);
        balance.debitAvailable(new BigDecimal("30.00"), now.plusSeconds(2));
        
        assertEquals(new BigDecimal("70.00"), balance.getAvailable());
    }

    @Test
    void shouldFailToDebitMoreThanAvailable() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        assertThrows(PaymentException.class, () -> balance.debitAvailable(new BigDecimal("10.00"), now));
    }

    @Test
    void shouldCreditAvailableCorrectly() {
        Instant now = Instant.now();
        MerchantBalance balance = MerchantBalance.empty("m-1", "VND", now);
        
        balance.creditAvailable(new BigDecimal("50.00"), now.plusSeconds(1));
        assertEquals(new BigDecimal("50.00"), balance.getAvailable());
    }
}
