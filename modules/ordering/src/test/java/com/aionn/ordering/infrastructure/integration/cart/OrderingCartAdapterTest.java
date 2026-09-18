package com.aionn.ordering.infrastructure.integration.cart;

import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.ordering.CartOperationsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderingCartAdapterTest {

    @Mock
    private CartPersistencePort cartRepository;

    @Mock
    private EventPublisher eventPublisher;

    private Clock clock;
    private OrderingCartAdapter adapter;
    private final Instant now = Instant.parse("2026-09-18T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        adapter = new OrderingCartAdapter(cartRepository, eventPublisher, clock);
    }

    @Test
    void getOrCreateCartReturnsExistingCart() {
        Cart cart = Cart.create("cart-1", "user-1", now);
        cart.addItem("sku-1", 2, now);
        when(cartRepository.findById("cart-1")).thenReturn(Optional.of(cart));

        CartOperationsPort.CartSnapshot snapshot = adapter.getOrCreateCart("cart-1", "user-1");

        assertThat(snapshot.cartId()).isEqualTo("cart-1");
        assertThat(snapshot.userId()).isEqualTo("user-1");
        assertThat(snapshot.items()).containsEntry("sku-1", 2);
    }

    @Test
    void getOrCreateCartCreatesWhenAbsent() {
        Cart newCart = Cart.create("cart-2", "user-2", now);
        when(cartRepository.findById("cart-2")).thenReturn(Optional.empty());
        when(cartRepository.findOrCreate("cart-2", "user-2", now)).thenReturn(newCart);

        CartOperationsPort.CartSnapshot snapshot = adapter.getOrCreateCart("cart-2", "user-2");

        assertThat(snapshot.cartId()).isEqualTo("cart-2");
        assertThat(snapshot.userId()).isEqualTo("user-2");
        assertThat(snapshot.items()).isEmpty();
    }

    @Test
    void findCartByIdReturnsPresentSnapshot() {
        Cart cart = Cart.create("cart-3", "user-3", now);
        when(cartRepository.findById("cart-3")).thenReturn(Optional.of(cart));

        Optional<CartOperationsPort.CartSnapshot> result = adapter.findCartById("cart-3");

        assertThat(result).isPresent();
        assertThat(result.get().cartId()).isEqualTo("cart-3");
    }

    @Test
    void findCartByIdReturnsEmptyWhenNotFound() {
        when(cartRepository.findById("cart-unknown")).thenReturn(Optional.empty());

        Optional<CartOperationsPort.CartSnapshot> result = adapter.findCartById("cart-unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void saveCartItemsReplacesItemsAndSaves() {
        Cart cart = Cart.create("cart-4", "user-4", now);
        cart.addItem("old-sku", 1, now);
        when(cartRepository.findById("cart-4")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartOperationsPort.CartSnapshot snapshot = adapter.saveCartItems("cart-4", "user-4", Map.of("new-sku", 3));

        assertThat(snapshot.items()).containsOnly(Map.entry("new-sku", 3));
        verify(cartRepository).save(cart);
        verify(eventPublisher).publish(
                org.mockito.ArgumentMatchers.<java.util.Collection<com.aionn.sharedkernel.domain.model.EventEnvelope>>any());
    }

    @Test
    void clearCartRemovesAllItems() {
        Cart cart = Cart.create("cart-5", "user-5", now);
        cart.addItem("sku-1", 5, now);
        when(cartRepository.findById("cart-5")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartOperationsPort.CartSnapshot snapshot = adapter.clearCart("cart-5", "user-5");

        assertThat(snapshot.items()).isEmpty();
        verify(cartRepository).save(cart);
        verify(eventPublisher).publish(
                org.mockito.ArgumentMatchers.<java.util.Collection<com.aionn.sharedkernel.domain.model.EventEnvelope>>any());
    }
}
