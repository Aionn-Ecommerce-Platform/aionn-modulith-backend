package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.ApplyVoucherCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;

import com.aionn.ordering.application.port.in.cart.ApplyVoucherInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplyVoucherUseCase implements ApplyVoucherInputPort {

    private final CartService cartService;

    @Override
    public CartResult execute(ApplyVoucherCommand command) {
        return cartService.applyVoucher(command);
    }
}