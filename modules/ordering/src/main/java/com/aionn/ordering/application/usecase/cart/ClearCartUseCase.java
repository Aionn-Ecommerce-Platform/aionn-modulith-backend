package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.ClearCartCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;

import com.aionn.ordering.application.port.in.cart.ClearCartInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClearCartUseCase implements ClearCartInputPort {

    private final CartService cartService;

    @Override
    public CartResult execute(ClearCartCommand command) {
        return cartService.clearCart(command);
    }
}