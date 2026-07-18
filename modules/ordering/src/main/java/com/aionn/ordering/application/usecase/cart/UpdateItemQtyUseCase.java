package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.UpdateItemQtyCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;

import com.aionn.ordering.application.port.in.cart.UpdateItemQtyInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateItemQtyUseCase implements UpdateItemQtyInputPort {

    private final CartService cartService;

    @Override
    public CartResult execute(UpdateItemQtyCommand command) {
        return cartService.updateItemQty(command);
    }
}