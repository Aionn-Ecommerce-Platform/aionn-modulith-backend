package com.aionn.ordering.application.service;

import com.aionn.ordering.application.dto.cart.command.AddItemCommand;
import com.aionn.ordering.application.dto.cart.command.ApplyVoucherCommand;
import com.aionn.ordering.application.dto.cart.command.ClearCartCommand;
import com.aionn.ordering.application.dto.cart.command.RemoveItemCommand;
import com.aionn.ordering.application.dto.cart.command.RemoveVoucherCommand;
import com.aionn.ordering.application.dto.cart.command.UpdateItemQtyCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.mapper.OrderingResultMapper;
import com.aionn.ordering.application.port.out.CartPersistencePort;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Cart;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartPersistencePort cartRepository;
    private final OrderingResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final java.time.Clock clock;

    public CartResult addItem(AddItemCommand command) {
        Cart cart = loadOrCreate(command.userId());
        cart.addItem(command.skuId(), command.qty(), clock.instant());
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return mapper.toResult(saved);
    }

    public CartResult updateItemQty(UpdateItemQtyCommand command) {
        Cart cart = loadOwned(command.userId());
        cart.updateItemQty(command.skuId(), command.newQty(), clock.instant());
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return mapper.toResult(saved);
    }

    public CartResult removeItem(RemoveItemCommand command) {
        Cart cart = loadOwned(command.userId());
        cart.removeItem(command.skuId(), clock.instant());
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return mapper.toResult(saved);
    }

    public CartResult clearCart(ClearCartCommand command) {
        Cart cart = loadOwned(command.userId());
        cart.clear(command.reason(), clock.instant());
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return mapper.toResult(saved);
    }

    public CartResult applyVoucher(ApplyVoucherCommand command) {
        Cart cart = loadOwned(command.userId());
        cart.applyVoucher(command.voucherCode(), clock.instant());
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return mapper.toResult(saved);
    }

    public CartResult removeVoucher(RemoveVoucherCommand command) {
        Cart cart = loadOwned(command.userId());
        cart.removeVoucher(clock.instant());
        Cart saved = cartRepository.save(cart);
        eventPublisher.publish(cart.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public CartResult getMyCart(String userId) {
        return mapper.toResult(loadOrCreate(userId));
    }

    Cart loadOwned(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.CART_NOT_FOUND));
        cart.ensureOwnedBy(userId);
        return cart;
    }

    private Cart loadOrCreate(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = Cart.create(IdGenerator.ulid(), userId, clock.instant());
            return cartRepository.save(cart);
        });
    }
}
