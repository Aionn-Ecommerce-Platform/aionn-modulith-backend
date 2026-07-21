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

        Cart result = adapter.save(cart);

        assertThat(result).isEqualTo(savedCart);
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

        Cart result = adapter.save(cart);

        assertThat(result).isEqualTo(savedCart);
        verify(jpa).findById("cart-1");
        verify(mapper).toEntity(cart, existingEntity);
        verify(jpa).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findsCartByIdWhenExists() {
        String cartId = "cart-1";
        CartEntity entity = mock(CartEntity.class);
        Cart cart = mock(Cart.class);

        when(jpa.findById(cartId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(cart);

        Optional<Cart> result = adapter.findById(cartId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(cart);
        verify(jpa).findById(cartId);
        verify(mapper).toDomain(entity);
    }

    @Test
    void returnsEmptyWhenCartNotFoundById() {
        String cartId = "non-existent";

        when(jpa.findById(cartId)).thenReturn(Optional.empty());

        Optional<Cart> result = adapter.findById(cartId);

        assertThat(result).isEmpty();
        verify(jpa).findById(cartId);
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void findsCartByUserIdWhenExists() {
        String userId = "user-1";
        CartEntity entity = mock(CartEntity.class);
        Cart cart = mock(Cart.class);

        when(jpa.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(cart);

        Optional<Cart> result = adapter.findByUserId(userId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(cart);
        verify(jpa).findByUserId(userId);
        verify(mapper).toDomain(entity);
    }

    @Test
    void returnsEmptyWhenCartNotFoundByUserId() {
        String userId = "user-1";

        when(jpa.findByUserId(userId)).thenReturn(Optional.empty());

        Optional<Cart> result = adapter.findByUserId(userId);

        assertThat(result).isEmpty();
        verify(jpa).findByUserId(userId);
        verify(mapper, never()).toDomain(any());
    }
}
