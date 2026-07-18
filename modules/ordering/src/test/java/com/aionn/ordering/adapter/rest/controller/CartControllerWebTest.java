package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.response.CartResponse;
import com.aionn.ordering.adapter.rest.exception.OrderingExceptionHandler;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.ordering.application.dto.cart.command.*;
import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.port.in.cart.*;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.sharedkernel.adapter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerWebTest {

    @Mock private AddItemInputPort addItemInputPort;
    @Mock private UpdateItemQtyInputPort updateItemQtyInputPort;
    @Mock private RemoveItemInputPort removeItemInputPort;
    @Mock private ClearCartInputPort clearCartInputPort;
    @Mock private ApplyVoucherInputPort applyVoucherInputPort;
    @Mock private RemoveVoucherInputPort removeVoucherInputPort;
    @Mock private GetMyCartInputPort getMyCartInputPort;
    @Mock private OrderingDtoMapper dtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CartController controller = new CartController(
                addItemInputPort, updateItemQtyInputPort, removeItemInputPort, clearCartInputPort,
                applyVoucherInputPort, removeVoucherInputPort, getMyCartInputPort, dtoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new OrderingExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user-1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static CartResult sampleCart(int itemsCount) {
        List<CartResult.CartItemResult> items = itemsCount > 0 
                ? List.of(new CartResult.CartItemResult("sku-1", itemsCount)) 
                : List.of();
        return new CartResult("cart-1", "user-1", items, "VOUCHER10", Instant.now(), Instant.now());
    }

    private static CartResponse sampleResponse(int itemsCount) {
        List<CartResponse.CartItemResponse> items = itemsCount > 0 
                ? List.of(new CartResponse.CartItemResponse("sku-1", itemsCount)) 
                : List.of();
        return new CartResponse("cart-1", "user-1", items, "VOUCHER10", Instant.now(), Instant.now());
    }

    @Test
    void getMyCartReturnsSuccess() throws Exception {
        CartResult result = sampleCart(1);
        when(getMyCartInputPort.execute("user-1")).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse(1));

        mockMvc.perform(get("/api/v1/ordering/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.data.cartId").value("cart-1"));
    }

    @Test
    void getMyCartUnauthenticatedFails() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/ordering/cart"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(getMyCartInputPort);
    }

    @Test
    void addItemReturnsSuccess() throws Exception {
        CartResult result = sampleCart(1);
        when(addItemInputPort.execute(any(AddItemCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse(1));

        mockMvc.perform(post("/api/v1/ordering/cart/items")
                        .contentType(APPLICATION_JSON)
                        .content("{\"skuId\":\"sku-1\",\"qty\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"));

        verify(addItemInputPort).execute(any());
    }

    @Test
    void addItemWithNegativeQtyFailsValidation() throws Exception {
        mockMvc.perform(post("/api/v1/ordering/cart/items")
                        .contentType(APPLICATION_JSON)
                        .content("{\"skuId\":\"sku-1\",\"qty\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("400"));

        verifyNoInteractions(addItemInputPort);
    }

    @Test
    void addItemBusinessErrorPropagated() throws Exception {
        when(addItemInputPort.execute(any(AddItemCommand.class)))
                .thenThrow(new OrderingException(OrderingErrorCode.INVALID_ARGUMENT,
                        "Quantity per item cannot exceed 999"));

        mockMvc.perform(post("/api/v1/ordering/cart/items")
                        .contentType(APPLICATION_JSON)
                        .content("{\"skuId\":\"sku-1\",\"qty\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.errorCode").value("ORD_900"));
    }

    @Test
    void addItemForbiddenPropagated() throws Exception {
        when(addItemInputPort.execute(any(AddItemCommand.class)))
                .thenThrow(new OrderingException(OrderingErrorCode.CART_FORBIDDEN));

        mockMvc.perform(post("/api/v1/ordering/cart/items")
                        .contentType(APPLICATION_JSON)
                        .content("{\"skuId\":\"sku-1\",\"qty\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateItemReturnsSuccess() throws Exception {
        CartResult result = sampleCart(1);
        when(updateItemQtyInputPort.execute(any(UpdateItemQtyCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse(1));

        mockMvc.perform(put("/api/v1/ordering/cart/items/sku-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"newQty\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"));
    }

    @Test
    void updateItemWithMissingQtyFailsValidation() throws Exception {
        mockMvc.perform(put("/api/v1/ordering/cart/items/sku-1")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItemNotFoundPropagated() throws Exception {
        when(updateItemQtyInputPort.execute(any(UpdateItemQtyCommand.class)))
                .thenThrow(new OrderingException(OrderingErrorCode.CART_ITEM_NOT_FOUND));

        mockMvc.perform(put("/api/v1/ordering/cart/items/sku-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"newQty\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItemReturnsSuccess() throws Exception {
        CartResult result = sampleCart(0);
        when(removeItemInputPort.execute(any(RemoveItemCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse(0));

        mockMvc.perform(delete("/api/v1/ordering/cart/items/sku-1"))
                .andExpect(status().isOk());

        verify(removeItemInputPort).execute(any());
    }

    @Test
    void removeItemNotFoundPropagated() throws Exception {
        when(removeItemInputPort.execute(any(RemoveItemCommand.class)))
                .thenThrow(new OrderingException(OrderingErrorCode.CART_ITEM_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/ordering/cart/items/sku-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearCartReturnsSuccess() throws Exception {
        CartResult result = sampleCart(0);
        when(clearCartInputPort.execute(any(ClearCartCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse(0));

        mockMvc.perform(delete("/api/v1/ordering/cart"))
                .andExpect(status().isOk());

        verify(clearCartInputPort).execute(any());
    }

    @Test
    void applyVoucherReturnsSuccess() throws Exception {
        CartResult result = sampleCart(1);
        when(applyVoucherInputPort.execute(any(ApplyVoucherCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse(1));

        mockMvc.perform(post("/api/v1/ordering/cart/voucher")
                        .contentType(APPLICATION_JSON)
                        .content("{\"voucherCode\":\"SAVE10\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void removeVoucherReturnsSuccess() throws Exception {
        CartResult result = sampleCart(1);
        when(removeVoucherInputPort.execute(any(RemoveVoucherCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse(1));

        mockMvc.perform(delete("/api/v1/ordering/cart/voucher"))
                .andExpect(status().isOk());
    }
}
