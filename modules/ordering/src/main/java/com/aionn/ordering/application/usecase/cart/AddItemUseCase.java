package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.AddItemCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;

import com.aionn.ordering.application.port.in.cart.AddItemInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddItemUseCase implements AddItemInputPort {

    private final CartService cartService;

    @Override
    public CartResult execute(AddItemCommand command) {
        return cartService.addItem(command);
    }
}