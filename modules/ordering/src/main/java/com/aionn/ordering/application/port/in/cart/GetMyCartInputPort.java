package com.aionn.ordering.application.port.in.cart;


import com.aionn.ordering.application.dto.cart.result.CartResult;


public interface GetMyCartInputPort {
    CartResult execute(String command);
}