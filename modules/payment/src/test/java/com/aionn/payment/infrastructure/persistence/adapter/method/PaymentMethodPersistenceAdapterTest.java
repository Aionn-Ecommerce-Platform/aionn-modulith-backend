package com.aionn.payment.infrastructure.persistence.adapter.method;

import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.infrastructure.persistence.entity.PaymentMethodEntity;
import com.aionn.payment.infrastructure.persistence.mapper.PaymentMethodDomainMapper;
import com.aionn.payment.infrastructure.persistence.repository.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMethodPersistenceAdapterTest {

    @Mock
    private PaymentMethodRepository repository;
    @Mock
    private PaymentMethodDomainMapper mapper;

    private PaymentMethodPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentMethodPersistenceAdapter(repository, mapper);
    }

    @Test
    void shouldFindById() {
        PaymentMethodEntity entity = new PaymentMethodEntity();
        PaymentMethod domain = mock(PaymentMethod.class);

        when(repository.findById("method-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<PaymentMethod> result = adapter.findById("method-1");

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
    }

    @Test
    void shouldFindActiveByUserId() {
        PaymentMethodEntity entity = new PaymentMethodEntity();
        PaymentMethod domain = mock(PaymentMethod.class);

        when(repository.findByUserIdAndStatusNot(eq("user-1"), any())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<PaymentMethod> results = adapter.findActiveByUserId("user-1");

        assertEquals(1, results.size());
        assertEquals(domain, results.get(0));
    }

    @Test
    void shouldSavePaymentMethod() {
        PaymentMethod domain = mock(PaymentMethod.class);
        PaymentMethodEntity entity = new PaymentMethodEntity();

        when(mapper.toEntity(domain, null)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        PaymentMethod result = adapter.save(domain);

        assertEquals(domain, result);
    }
}
