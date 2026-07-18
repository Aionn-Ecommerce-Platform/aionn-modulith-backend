package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.cart.command.*;
import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.mapper.OrderingResultMapper;
import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String USER_ID = "user-123";
    private static final String CART_ID = "cart-1";

    @Mock private CartPersistencePort cartRepository;
    @Mock private OrderingResultMapper mapper;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private CartService cartService;

    private Cart emptyCart() {
        return new Cart(CART_ID, USER_ID, new HashMap<>(), null, Instant.now(), Instant.now());
    }

    private Cart cartWithItem(String skuId, int qty) {
        Map<String, Integer> items = new HashMap<>();
        items.put(skuId, qty);
        return new Cart(CART_ID, USER_ID, items, null, Instant.now(), Instant.now());
    }

    private CartResult sampleCartResult() {
        return new CartResult(CART_ID, USER_ID, List.of(), null, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("removeVoucher() should clear voucher from cart and save changes")
    void removeVoucher_clearsVoucher_whenCartExists() {
        Cart cart = new Cart(CART_ID, USER_ID, new HashMap<>(), "DISCOUNT50", Instant.now(), Instant.now());
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        cartService.removeVoucher(new RemoveVoucherCommand(USER_ID));

        assertNull(cart.getVoucherCode());
        verify(cartRepository).save(cart);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    @DisplayName("addItem() should add item and save cart")
    void addItem_addsItemToCart() {
        Cart cart = emptyCart();
        Cart saved = cartWithItem("sku-1", 2);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(saved);
        when(mapper.toResult(saved)).thenReturn(sampleCartResult());

        CartResult result = cartService.addItem(new AddItemCommand(USER_ID, "sku-1", 2));

        assertNotNull(result);
        verify(cartRepository).save(any());
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    @DisplayName("addItem() creates new cart if none exists")
    void addItem_createsNewCartIfNoneExists() {
        Cart newCart = emptyCart();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(mapper.toResult(newCart)).thenReturn(sampleCartResult());

        CartResult result = cartService.addItem(new AddItemCommand(USER_ID, "sku-1", 1));

        assertNotNull(result);
        verify(cartRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("updateItemQty() updates quantity")
    void updateItemQty_updatesExistingItem() {
        Cart cart = cartWithItem("sku-1", 1);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(mapper.toResult(cart)).thenReturn(sampleCartResult());

        CartResult result = cartService.updateItemQty(new UpdateItemQtyCommand(USER_ID, "sku-1", 5));

        assertNotNull(result);
    }

    @Test
    @DisplayName("updateItemQty() throws if cart not found")
    void updateItemQty_throwsWhenCartNotFound() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        assertThrows(OrderingException.class,
                () -> cartService.updateItemQty(new UpdateItemQtyCommand(USER_ID, "sku-1", 5)));
    }

    @Test
    @DisplayName("removeItem() removes item from cart")
    void removeItem_removesItemFromCart() {
        Cart cart = cartWithItem("sku-1", 3);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(mapper.toResult(cart)).thenReturn(sampleCartResult());

        CartResult result = cartService.removeItem(new RemoveItemCommand(USER_ID, "sku-1"));

        assertNotNull(result);
    }

    @Test
    @DisplayName("clearCart() clears all items")
    void clearCart_clearsAllItems() {
        Cart cart = cartWithItem("sku-1", 2);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(mapper.toResult(cart)).thenReturn(sampleCartResult());

        CartResult result = cartService.clearCart(new ClearCartCommand(USER_ID, "reason"));

        assertNotNull(result);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    @DisplayName("applyVoucher() applies voucher code")
    void applyVoucher_appliesVoucherCode() {
        Cart cart = emptyCart();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(mapper.toResult(cart)).thenReturn(sampleCartResult());

        CartResult result = cartService.applyVoucher(new ApplyVoucherCommand(USER_ID, "PROMO10"));

        assertNotNull(result);
        assertEquals("PROMO10", cart.getVoucherCode());
    }

    @Test
    @DisplayName("getMyCart() returns existing cart")
    void getMyCart_returnsExistingCart() {
        Cart cart = emptyCart();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(mapper.toResult(cart)).thenReturn(sampleCartResult());

        CartResult result = cartService.getMyCart(USER_ID);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getMyCart() creates cart if none exists")
    void getMyCart_createsCartIfNoneExists() {
        Cart newCart = emptyCart();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(mapper.toResult(newCart)).thenReturn(sampleCartResult());

        CartResult result = cartService.getMyCart(USER_ID);

        assertNotNull(result);
        verify(cartRepository).save(any());
    }
}
