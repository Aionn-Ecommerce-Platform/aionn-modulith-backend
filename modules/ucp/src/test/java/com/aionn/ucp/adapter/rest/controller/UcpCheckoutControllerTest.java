package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.advice.UcpControllerAdvice;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutRequest;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpLinkResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpOrderConfirmationResponse;
import com.aionn.ucp.application.checkout.UcpCheckoutApplicationService;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UcpCheckoutControllerTest {

    @Mock
    private UcpCheckoutApplicationService checkoutService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UcpCheckoutController(checkoutService))
                .setControllerAdvice(new UcpControllerAdvice())
                .build();
    }

    private UcpCheckoutResponse sampleResponse(String checkoutId, String status) {
        return new UcpCheckoutResponse(
                UcpResponseMetadata.success("2026-08-25"),
                checkoutId,
                status,
                "USD",
                List.of(new UcpLineItemResponse(
                        "li_1",
                        new UcpItemResponse("sku-1", "Sample Item", 2000L, null),
                        1,
                        List.of(UcpTotalResponse.subtotal(2000L), UcpTotalResponse.total(2000L)))),
                List.of(UcpTotalResponse.subtotal(2000L), UcpTotalResponse.total(2000L)),
                List.of(UcpLinkResponse.termsOfService("https://aionn.vn")),
                null,
                null,
                "completed".equals(status) ? UcpOrderConfirmationResponse.of("ord-123", "https://aionn.vn") : null,
                "2026-09-18T16:00:00Z",
                null);
    }

    @Test
    void createCheckoutReturns201WithResponse() throws Exception {
        when(checkoutService.createCheckout(any(UcpCheckoutRequest.class), any()))
                .thenReturn(sampleResponse("chk_123", "incomplete"));

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

        mockMvc.perform(post("/ucp/v1/checkout-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ucp.version").value("2026-08-25"))
                .andExpect(jsonPath("$.id").value("chk_123"))
                .andExpect(jsonPath("$.status").value("incomplete"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getCheckoutReturns200WithResponse() throws Exception {
        when(checkoutService.getCheckout(eq("chk_123"), any()))
                .thenReturn(sampleResponse("chk_123", "incomplete"));

        mockMvc.perform(get("/ucp/v1/checkout-sessions/chk_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("chk_123"))
                .andExpect(jsonPath("$.status").value("incomplete"));
    }

    @Test
    void getCheckoutNotFoundReturnsCanonicalUcpError() throws Exception {
        when(checkoutService.getCheckout(eq("chk_unknown"), any()))
                .thenThrow(new UcpProtocolException(404, "checkout_not_found", "Checkout session not found: chk_unknown", "error", "$.id"));

        mockMvc.perform(get("/ucp/v1/checkout-sessions/chk_unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ucp.status").value("error"))
                .andExpect(jsonPath("$.messages[0].code").value("checkout_not_found"));
    }

    @Test
    void updateCheckoutReturns200WithUpdatedResponse() throws Exception {
        when(checkoutService.updateCheckout(eq("chk_123"), any(UcpCheckoutRequest.class), any()))
                .thenReturn(sampleResponse("chk_123", "incomplete"));

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

        mockMvc.perform(put("/ucp/v1/checkout-sessions/chk_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("chk_123"));
    }

    @Test
    void completeCheckoutReturns200WithCompletedResponse() throws Exception {
        when(checkoutService.completeCheckout(eq("chk_123"), any()))
                .thenReturn(sampleResponse("chk_123", "completed"));

        mockMvc.perform(post("/ucp/v1/checkout-sessions/chk_123/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.order.id").value("ord-123"));
    }

    @Test
    void cancelCheckoutReturns200WithCanceledResponse() throws Exception {
        when(checkoutService.cancelCheckout(eq("chk_123"), any()))
                .thenReturn(sampleResponse("chk_123", "canceled"));

        mockMvc.perform(post("/ucp/v1/checkout-sessions/chk_123/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("canceled"));
    }
}
