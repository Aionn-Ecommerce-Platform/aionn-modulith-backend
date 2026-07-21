package com.aionn.payment.infrastructure.persistence.adapter.settlement;

import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.infrastructure.persistence.entity.MerchantBalanceEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantBalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantBalancePersistenceAdapterTest {

    @Mock
    private MerchantBalanceRepository jpa;

    @InjectMocks
    private MerchantBalancePersistenceAdapter adapter;

    @Test
    void shouldFindAndSaveMerchantBalance() {
        MerchantBalanceEntity entity = MerchantBalanceEntity.builder()
                .merchantId("merch-1")
                .currency("VND")
                .pending(BigDecimal.ZERO)
                .available(BigDecimal.valueOf(100000))
                .build();

        when(jpa.findByMerchantAndCurrency("merch-1", "VND")).thenReturn(Optional.of(entity));
        when(jpa.save(any())).thenReturn(entity);

        Optional<MerchantBalance> result = adapter.find("merch-1", "VND");
        assertTrue(result.isPresent());

        MerchantBalance saved = adapter.save(result.get());
        assertNotNull(saved);
        assertEquals("merch-1", saved.getMerchantId());
    }
}
