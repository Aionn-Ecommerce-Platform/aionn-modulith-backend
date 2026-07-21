package com.aionn.payment.infrastructure.persistence.adapter.payment;

import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.infrastructure.persistence.entity.PaymentEntity;
import com.aionn.payment.infrastructure.persistence.mapper.PaymentDomainMapper;
import com.aionn.payment.infrastructure.persistence.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPersistenceAdapterTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentDomainMapper paymentDomainMapper;

    private PaymentPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentPersistenceAdapter(paymentRepository, paymentDomainMapper);
    }

    @Test
    void shouldFindById() {
        PaymentEntity entity = new PaymentEntity();
        Payment domain = mock(Payment.class);

        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(entity));
        when(paymentDomainMapper.toDomain(entity)).thenReturn(domain);

        Optional<Payment> result = adapter.findById("pay-1");

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    void shouldFindByIdempty() {
        when(paymentRepository.findById("pay-2")).thenReturn(Optional.empty());

        Optional<Payment> result = adapter.findById("pay-2");

        assertFalse(result.isPresent());
    }

    @Test
    void shouldSavePayment() {
        Payment domain = mock(Payment.class);
        PaymentEntity entity = new PaymentEntity();

        when(paymentDomainMapper.toEntity(domain, null)).thenReturn(entity);
        when(paymentRepository.save(entity)).thenReturn(entity);
        when(paymentDomainMapper.toDomain(entity)).thenReturn(domain);

        Payment result = adapter.save(domain);

        assertEquals(domain, result);
    }

    @Test
    void shouldFindByIdempotencyKey() {
        PaymentEntity entity = new PaymentEntity();
        Payment domain = mock(Payment.class);

        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(entity));
        when(paymentDomainMapper.toDomain(entity)).thenReturn(domain);

        Optional<Payment> result = adapter.findByIdempotencyKey("idem-1");

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    void shouldFindByOrderId() {
        PaymentEntity entity = new PaymentEntity();
        Payment domain = mock(Payment.class);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(java.util.List.of(entity));
        when(paymentDomainMapper.toDomain(entity)).thenReturn(domain);

        java.util.List<Payment> results = adapter.findByOrderId("order-1");

        assertEquals(1, results.size());
        assertEquals(domain, results.get(0));
    }
}
