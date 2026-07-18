package com.aionn.ordering.application.mapper;

import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.domain.model.Cart;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class CartResultMapperTest {

    private final CartResultMapper mapper = Mappers.getMapper(CartResultMapper.class);

    @Test
    void mapsItemsAndVoucher() {
        Cart cart = Cart.create("cart-1", "user-1");
        cart.addItem("sku-1", 3);
        cart.applyVoucher("WELCOME");

        CartResult result = mapper.toResult(cart);

        assertThat(result.cartId()).isEqualTo("cart-1");
        assertThat(result.userId()).isEqualTo("user-1");
        assertThat(result.voucherCode()).isEqualTo("WELCOME");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).qty()).isEqualTo(3);
    }
}
