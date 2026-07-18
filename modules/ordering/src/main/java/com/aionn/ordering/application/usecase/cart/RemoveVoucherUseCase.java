package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.RemoveVoucherCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;

import com.aionn.ordering.application.port.in.cart.RemoveVoucherInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoveVoucherUseCase implements RemoveVoucherInputPort {

    private final CartService cartService;

    @Override
    public CartResult execute(RemoveVoucherCommand command) {
        return cartService.removeVoucher(command);
    }
}