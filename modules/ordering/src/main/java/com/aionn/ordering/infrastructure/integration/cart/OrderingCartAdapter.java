package com.aionn.ordering.infrastructure.integration.cart;

import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.ordering.CartOperationsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Ordering-side adapter implementing CartOperationsPort.
 * Safely bridges protocol callers (such as UCP) to the Ordering module's
 * Cart persistence and domain lifecycle.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class OrderingCartAdapter implements CartOperationsPort {

    private final CartPersistencePort cartRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public CartSnapshot getOrCreateCart(String cartId, String userId) {
        Optional<Cart> existing = cartRepository.findById(cartId);
        if (existing.isPresent()) {
            return toSnapshot(existing.get());
        }
        Instant now = clock.instant();
        Cart cart = cartRepository.findOrCreate(cartId, userId, now);
        return toSnapshot(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CartSnapshot> findCartById(String cartId) {
        return cartRepository.findById(cartId).map(this::toSnapshot);
    }

    @Override
    public CartSnapshot saveCartItems(String cartId, String userId, Map<String, Integer> skuQuantities) {
        Instant now = clock.instant();
        Cart cart = cartRepository.findByIdForUpdate(cartId)
                .orElseGet(() -> cartRepository.findOrCreate(cartId, userId, now));

        cart.clear("ucp_cart_update", now);
        if (skuQuantities != null) {
            skuQuantities.forEach((skuId, qty) -> {
                if (qty != null && qty > 0) {
                    cart.addItem(skuId, qty, now);
                }
            });
        }
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return toSnapshot(saved);
    }

    @Override
    public CartSnapshot clearCart(String cartId, String userId) {
        Instant now = clock.instant();
        Cart cart = cartRepository.findByIdForUpdate(cartId)
                .orElseGet(() -> cartRepository.findOrCreate(cartId, userId, now));

        cart.clear("ucp_cart_cancel", now);
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return toSnapshot(saved);
    }

    private CartSnapshot toSnapshot(Cart cart) {
        return new CartSnapshot(
                cart.getCartId(),
                cart.getUserId(),
                cart.getItems(),
                cart.getVoucherCode(),
                cart.getCreatedAt(),
                cart.getUpdatedAt());
    }
}
