package com.aionn.ordering.domain.model;

import com.aionn.ordering.domain.event.CartEvents;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private static final String CART = "cart-1";
    private static final String USER = "user-1";

    @Test
    void createIsEmpty() {
        Cart cart = Cart.create(CART, USER);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.peekEvents()).isEmpty();
    }

    @Test
    void addItemAccumulatesQty() {
        Cart cart = Cart.create(CART, USER);
        cart.addItem("sku-1", 2);
        cart.addItem("sku-1", 3);

        assertThat(cart.snapshot()).hasSize(1);
        assertThat(cart.snapshot().get(0).getValue()).isEqualTo(5);
        assertThat(cart.peekEvents()).hasSize(2);
    }

    @Test
    void addItemRejectsZeroQty() {
        Cart cart = Cart.create(CART, USER);

        assertThatThrownBy(() -> cart.addItem("sku-1", 0))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void updateItemToZeroRemoves() {
        Cart cart = Cart.create(CART, USER);
        cart.addItem("sku-1", 5);
        cart.pullEvents();

        cart.updateItemQty("sku-1", 0);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.peekEvents()).anyMatch(env -> env.payload() instanceof CartEvents.CartItemRemoved);
    }

    @Test
    void updateMissingItemRejected() {
        Cart cart = Cart.create(CART, USER);

        assertThatThrownBy(() -> cart.updateItemQty("sku-x", 1))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.CART_ITEM_NOT_FOUND.getCode());
    }

    @Test
    void removeMissingItemRejected() {
        Cart cart = Cart.create(CART, USER);

        assertThatThrownBy(() -> cart.removeItem("sku-x"))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.CART_ITEM_NOT_FOUND.getCode());
    }

    @Test
    void clearAlreadyEmptyDoesNotEmit() {
        Cart cart = Cart.create(CART, USER);

        cart.clear("noop");

        assertThat(cart.peekEvents()).isEmpty();
    }

    @Test
    void clearWithItemsEmitsCartCleared() {
        Cart cart = Cart.create(CART, USER);
        cart.addItem("sku-1", 1);
        cart.pullEvents();

        cart.clear("user-cleared");

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.peekEvents()).anyMatch(env -> env.payload() instanceof CartEvents.CartCleared);
    }

    @Test
    void applyVoucherTrimsAndStores() {
        Cart cart = Cart.create(CART, USER);

        cart.applyVoucher("  WELCOME10  ");

        assertThat(cart.getVoucherCode()).isEqualTo("WELCOME10");
    }

    @Test
    void applyBlankVoucherRejected() {
        Cart cart = Cart.create(CART, USER);

        assertThatThrownBy(() -> cart.applyVoucher(" "))
                .isInstanceOf(OrderingException.class);
    }

    @Test
    void removeVoucherClearsAndEmitsRemoved() {
        Cart cart = Cart.create(CART, USER);
        cart.applyVoucher("X");
        cart.pullEvents();

        cart.removeVoucher();

        assertThat(cart.getVoucherCode()).isNull();
        assertThat(cart.peekEvents()).anyMatch(env -> env.payload() instanceof CartEvents.VoucherRemoved);
    }

    @Test
    void ensureOwnedByOtherUserForbidden() {
        Cart cart = Cart.create(CART, USER);

        assertThatThrownBy(() -> cart.ensureOwnedBy("OTHER"))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.CART_FORBIDDEN.getCode());
    }
}
