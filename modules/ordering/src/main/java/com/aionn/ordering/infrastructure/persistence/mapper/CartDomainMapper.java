package com.aionn.ordering.infrastructure.persistence.mapper;

import com.aionn.ordering.domain.model.Cart;
import com.aionn.ordering.infrastructure.persistence.entity.CartEntity;
import com.aionn.ordering.infrastructure.persistence.entity.CartItemEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CartDomainMapper {

    public Cart toDomain(CartEntity e) {
        Map<String, Integer> items = new LinkedHashMap<>();
        for (CartItemEntity item : e.getItems()) {
            items.put(item.getId().getSkuId(), item.getQty());
        }
        return new Cart(e.getCartId(), e.getUserId(), items, e.getVoucherCode(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    public CartEntity toEntity(Cart cart, CartEntity existing) {
        CartEntity entity = existing != null ? existing
                : CartEntity.builder()
                        .cartId(cart.getCartId())
                        .userId(cart.getUserId())
                        .build();
        entity.setVoucherCode(cart.getVoucherCode());
        entity.getItems().clear();
        for (Map.Entry<String, Integer> e : cart.snapshot()) {
            CartItemEntity item = CartItemEntity.builder()
                    .id(new CartItemEntity.CartItemId(cart.getCartId(), e.getKey()))
                    .cart(entity)
                    .qty(e.getValue())
                    .build();
            entity.getItems().add(item);
        }
        return entity;
    }
}

