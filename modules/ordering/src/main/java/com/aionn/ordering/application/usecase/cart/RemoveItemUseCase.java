package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.RemoveItemCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;

import com.aionn.ordering.application.port.in.cart.RemoveItemInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoveItemUseCase implements RemoveItemInputPort {

    private final CartService cartService;

    @Override
    public CartResult execute(RemoveItemCommand command) {
        return cartService.removeItem(command);
    }
}