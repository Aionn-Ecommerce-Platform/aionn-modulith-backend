package com.aionn.ordering.infrastructure.persistence.adapter.cart;

import com.aionn.ordering.domain.model.Cart;
import com.aionn.ordering.infrastructure.persistence.entity.CartEntity;
import com.aionn.ordering.infrastructure.persistence.mapper.CartDomainMapper;
import com.aionn.ordering.infrastructure.persistence.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartPersistenceAdapterTest {

    @Mock
    private CartRepository jpa;
    @Mock
    private CartDomainMapper mapper;
    @InjectMocks
    private CartPersistenceAdapter adapter;

    @Test
    void savesNewCartSuccessfully() {
        Cart cart = mock(Cart.class);
        CartEntity entity = mock(CartEntity.class);
        CartEntity savedEntity = mock(CartEntity.class);
        Cart savedCart = mock(Cart.class);
        when(cart.getCartId()).thenReturn("cart-1");
        when(jpa.findById("cart-1")).thenReturn(Optional.empty());
        when(mapper.toEntity(cart, null)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedCart);

        assertThat(adapter.save(cart)).isEqualTo(savedCart);
        verify(jpa).findById("cart-1");
        verify(mapper).toEntity(cart, null);
        verify(jpa).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void updatesExistingCartSuccessfully() {
        Cart cart = mock(Cart.class);
        CartEntity existingEntity = mock(CartEntity.class);
        CartEntity entity = mock(CartEntity.class);
        CartEntity savedEntity = mock(CartEntity.class);
        Cart savedCart = mock(Cart.class);
        when(cart.getCartId()).thenReturn("cart-1");
        when(jpa.findById("cart-1")).thenReturn(Optional.of(existingEntity));
        when(mapper.toEntity(cart, existingEntity)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedCart);

        assertThat(adapter.save(cart)).isEqualTo(savedCart);
        verify(jpa).findById("cart-1");
        verify(mapper).toEntity(cart, existingEntity);
        verify(jpa).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findsCartByIdWhenExists() {
        CartEntity entity = mock(CartEntity.class);
        Cart cart = mock(Cart.class);
        when(jpa.findById("cart-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(cart);

        assertThat(adapter.findById("cart-1")).contains(cart);
        verify(jpa).findById("cart-1");
        verify(mapper).toDomain(entity);
    }

    @Test
    void returnsEmptyWhenCartNotFoundById() {
        when(jpa.findById("non-existent")).thenReturn(Optional.empty());

        assertThat(adapter.findById("non-existent")).isEmpty();
        verify(jpa).findById("non-existent");
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void findsCartByUserIdWhenExists() {
        CartEntity entity = mock(CartEntity.class);
        Cart cart = mock(Cart.class);
        when(jpa.findByUserId("user-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(cart);

        assertThat(adapter.findByUserId("user-1")).contains(cart);
        verify(jpa).findByUserId("user-1");
        verify(mapper).toDomain(entity);
    }

    @Test
    void returnsEmptyWhenCartNotFoundByUserId() {
        when(jpa.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThat(adapter.findByUserId("user-1")).isEmpty();
        verify(jpa).findByUserId("user-1");
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void findOrCreateReturnsTheRowThatWonTheUserConstraint() {
        Instant now = Instant.parse("2026-08-05T00:00:00Z");
        CartEntity entity = mock(CartEntity.class);
        Cart cart = mock(Cart.class);
        when(jpa.findByUserId("user-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(cart);

        assertThat(adapter.findOrCreate("candidate-cart", "user-1", now)).isSameAs(cart);
        verify(jpa).insertIfAbsent("candidate-cart", "user-1", now);
        verify(jpa).findByUserId("user-1");
    }
}
