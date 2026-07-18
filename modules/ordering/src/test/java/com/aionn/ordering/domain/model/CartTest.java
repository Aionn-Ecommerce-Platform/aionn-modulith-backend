package com.aionn.ordering.domain.model;

import com.aionn.ordering.domain.event.CartEvents;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private static final String CART = "cart-1";
    private static final String USER = "user-1";
    private final Instant now = Instant.now();

    @Test
    void createIsEmpty() {
        Cart cart = Cart.create(CART, USER, now);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.peekEvents()).isEmpty();
    }

    @Test
    void addItemAccumulatesQty() {
        Cart cart = Cart.create(CART, USER, now);
        cart.addItem("sku-1", 2, now);
        cart.addItem("sku-1", 3, now);

        assertThat(cart.snapshot()).hasSize(1);
        assertThat(cart.snapshot().get(0).getValue()).isEqualTo(5);
        assertThat(cart.peekEvents()).hasSize(2);
    }

    @Test
    void addItemRejectsZeroQty() {
        Cart cart = Cart.create(CART, USER, now);

        assertThatThrownBy(() -> cart.addItem("sku-1", 0, now))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void updateItemToZeroRemoves() {
        Cart cart = Cart.create(CART, USER, now);
        cart.addItem("sku-1", 5, now);
        cart.pullEvents();

        cart.updateItemQty("sku-1", 0, now);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.peekEvents()).anyMatch(env -> env.payload() instanceof CartEvents.CartItemRemoved);
    }

    @Test
    void updateMissingItemRejected() {
        Cart cart = Cart.create(CART, USER, now);

        assertThatThrownBy(() -> cart.updateItemQty("sku-x", 1, now))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.CART_ITEM_NOT_FOUND.getCode());
    }

    @Test
    void removeMissingItemRejected() {
        Cart cart = Cart.create(CART, USER, now);

        assertThatThrownBy(() -> cart.removeItem("sku-x", now))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.CART_ITEM_NOT_FOUND.getCode());
    }

    @Test
    void clearAlreadyEmptyDoesNotEmit() {
        Cart cart = Cart.create(CART, USER, now);

        cart.clear("noop", now);

        assertThat(cart.peekEvents()).isEmpty();
    }

    @Test
    void clearWithItemsEmitsCartCleared() {
        Cart cart = Cart.create(CART, USER, now);
        cart.addItem("sku-1", 1, now);
        cart.pullEvents();

        cart.clear("user-cleared", now);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.peekEvents()).anyMatch(env -> env.payload() instanceof CartEvents.CartCleared);
    }

    @Test
    void applyVoucherTrimsAndStores() {
        Cart cart = Cart.create(CART, USER, now);

        cart.applyVoucher("  WELCOME10  ", now);

        assertThat(cart.getVoucherCode()).isEqualTo("WELCOME10");
    }

    @Test
    void applyBlankVoucherRejected() {
        Cart cart = Cart.create(CART, USER, now);

        assertThatThrownBy(() -> cart.applyVoucher(" ", now))
                .isInstanceOf(OrderingException.class);
    }

    @Test
    void removeVoucherClearsAndEmitsRemoved() {
        Cart cart = Cart.create(CART, USER, now);
        cart.applyVoucher("X", now);
        cart.pullEvents();

        cart.removeVoucher(now);

        assertThat(cart.getVoucherCode()).isNull();
        assertThat(cart.peekEvents()).anyMatch(env -> env.payload() instanceof CartEvents.VoucherRemoved);
    }

    @Test
    void ensureOwnedByOtherUserForbidden() {
        Cart cart = Cart.create(CART, USER, now);

        assertThatThrownBy(() -> cart.ensureOwnedBy("OTHER"))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.CART_FORBIDDEN.getCode());
    }
}