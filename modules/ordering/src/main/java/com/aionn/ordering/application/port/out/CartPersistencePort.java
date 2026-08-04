package com.aionn.ordering.application.port.out;

import com.aionn.ordering.domain.model.Cart;

import java.time.Instant;
import java.util.Optional;

public interface CartPersistencePort {

    Cart save(Cart cart);

    Optional<Cart> findById(String cartId);

    Optional<Cart> findByUserId(String userId);

    Cart findOrCreate(String cartId, String userId, Instant now);
}

