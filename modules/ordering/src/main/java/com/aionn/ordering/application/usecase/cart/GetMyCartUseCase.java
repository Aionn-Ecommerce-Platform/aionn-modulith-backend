package com.aionn.ordering.application.usecase.cart;


import com.aionn.ordering.application.dto.cart.result.CartResult;

import com.aionn.ordering.application.port.in.cart.GetMyCartInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMyCartUseCase implements GetMyCartInputPort {

    private final CartService cartService;

    @Override
    public CartResult execute(String command) {
        return cartService.getMyCart(command);
    }
}