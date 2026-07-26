package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.RemoveVoucherCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.mapper.CartResultMapper;
import com.aionn.ordering.application.port.in.cart.RemoveVoucherInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveVoucherUseCase implements RemoveVoucherInputPort {

    private final CartService cartService;
    private final CartResultMapper cartResultMapper;

    @Override
    @Transactional
    public CartResult execute(RemoveVoucherCommand command) {
        return cartResultMapper.toResult(cartService.removeVoucher(command));
    }
}
