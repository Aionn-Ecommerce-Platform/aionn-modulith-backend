package com.aionn.ordering.application.port.out.cart;

import com.aionn.ordering.domain.model.Cart;

import java.util.Optional;

public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findById(String cartId);

    Optional<Cart> findByUserId(String userId);
}
