package com.aionn.ordering.application.usecase.cart;

import com.aionn.ordering.application.dto.cart.command.AddItemCommand;
import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.mapper.CartResultMapper;
import com.aionn.ordering.application.port.in.cart.AddItemInputPort;
import com.aionn.ordering.application.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddItemUseCase implements AddItemInputPort {

    private final CartService cartService;
    private final CartResultMapper cartResultMapper;

    @Override
    @Transactional
    public CartResult execute(AddItemCommand command) {
        return cartResultMapper.toResult(cartService.addItem(command));
    }
}
