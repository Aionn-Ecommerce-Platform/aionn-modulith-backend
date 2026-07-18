package com.aionn.ordering.infrastructure.persistence.adapter.cart;

import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.ordering.infrastructure.persistence.entity.CartEntity;
import com.aionn.ordering.infrastructure.persistence.mapper.CartDomainMapper;
import com.aionn.ordering.infrastructure.persistence.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CartPersistenceAdapter implements CartPersistencePort {

    private final CartRepository jpa;
    private final CartDomainMapper mapper;

    @Override
    public Cart save(Cart cart) {
        CartEntity existing = jpa.findById(cart.getCartId()).orElse(null);
        CartEntity entity = mapper.toEntity(cart, existing);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Cart> findById(String cartId) {
        return jpa.findById(cartId).map(mapper::toDomain);
    }

    @Override
    public Optional<Cart> findByUserId(String userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }
}

