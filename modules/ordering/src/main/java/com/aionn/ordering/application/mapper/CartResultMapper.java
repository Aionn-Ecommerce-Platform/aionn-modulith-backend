package com.aionn.ordering.application.mapper;

import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.domain.model.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface CartResultMapper {

    @Mapping(target = "items", expression = "java(toItems(cart))")
    CartResult toResult(Cart cart);

    default List<CartResult.CartItemResult> toItems(Cart cart) {
        return cart.snapshot().stream()
                .map((Map.Entry<String, Integer> e) -> new CartResult.CartItemResult(e.getKey(), e.getValue()))
                .toList();
    }
}
