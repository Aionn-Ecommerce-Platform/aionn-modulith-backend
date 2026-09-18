package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.advice.UcpControllerAdvice;
import com.aionn.ucp.adapter.rest.dto.cart.UcpCartRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpCartResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.aionn.ucp.application.cart.UcpCartApplicationService;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UcpCartControllerTest {

  @Mock
  private UcpCartApplicationService cartService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new UcpCartController(cartService))
        .setControllerAdvice(new UcpControllerAdvice())
        .build();
  }

  private UcpCartResponse sampleResponse(String cartId) {
    return new UcpCartResponse(
        UcpResponseMetadata.success("2026-08-25"),
        cartId,
        "USD",
        List.of(new UcpLineItemResponse(
            "li_1",
            new UcpItemResponse("sku-1", "Sample Item", 1500L, null),
            1,
            List.of(UcpTotalResponse.subtotal(1500L), UcpTotalResponse.total(1500L)))),
        List.of(UcpTotalResponse.subtotal(1500L), UcpTotalResponse.total(1500L)),
        "2026-09-25T10:00:00Z",
        null);
  }

  @Test
  void createCartReturns201WithResponse() throws Exception {
    when(cartService.createCart(any(UcpCartRequest.class), any())).thenReturn(sampleResponse("cart_123"));

    String requestBody = """
        {
          "line_items": [
            {
              "item": { "id": "sku-1" },
              "quantity": 1
            }
          ]
        }
        """;

    mockMvc.perform(post("/ucp/v1/carts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ucp.version").value("2026-08-25"))
        .andExpect(jsonPath("$.id").value("cart_123"))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.totals[0].amount").value(1500));
  }

  @Test
  void getCartReturns200WithResponse() throws Exception {
    when(cartService.getCart(eq("cart_123"), any())).thenReturn(sampleResponse("cart_123"));

    mockMvc.perform(get("/ucp/v1/carts/cart_123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("cart_123"))
        .andExpect(jsonPath("$.line_items[0].item.id").value("sku-1"));
  }

  @Test
  void getCartNotFoundReturnsCanonicalUcpError() throws Exception {
    when(cartService.getCart(eq("cart_unknown"), any()))
        .thenThrow(new UcpProtocolException(404, "cart_not_found", "Cart not found: cart_unknown", "error", "$.id"));

    mockMvc.perform(get("/ucp/v1/carts/cart_unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.ucp.status").value("error"))
        .andExpect(jsonPath("$.messages[0].code").value("cart_not_found"))
        .andExpect(jsonPath("$.messages[0].path").value("$.id"));
  }

  @Test
  void updateCartReturns200WithUpdatedResponse() throws Exception {
    when(cartService.updateCart(eq("cart_123"), any(UcpCartRequest.class), any()))
        .thenReturn(sampleResponse("cart_123"));

    String requestBody = """
        {
          "line_items": [
            {
              "item": { "id": "sku-1" },
              "quantity": 2
            }
          ]
        }
        """;

    mockMvc.perform(put("/ucp/v1/carts/cart_123")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("cart_123"));
  }

  @Test
  void cancelCartReturns200WithResponse() throws Exception {
    when(cartService.cancelCart(eq("cart_123"), any())).thenReturn(sampleResponse("cart_123"));

    mockMvc.perform(post("/ucp/v1/carts/cart_123/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("cart_123"));
  }
}
