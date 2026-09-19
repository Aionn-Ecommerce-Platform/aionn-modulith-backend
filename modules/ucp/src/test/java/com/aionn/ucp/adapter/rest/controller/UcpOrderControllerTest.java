package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.advice.UcpControllerAdvice;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.aionn.ucp.adapter.rest.dto.order.UcpOrderModels.*;
import com.aionn.ucp.application.order.UcpOrderApplicationService;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UcpOrderControllerTest {

        @Mock
        private UcpOrderApplicationService orderService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .standaloneSetup(new UcpOrderController(orderService))
                                .setControllerAdvice(new UcpControllerAdvice())
                                .build();
        }

        private UcpOrderResponse sampleResponse(String orderId) {
                return new UcpOrderResponse(
                                UcpResponseMetadata.success("2026-08-25"),
                                orderId,
                                "Order #" + orderId,
                                "chk_" + orderId,
                                "https://aionn.vn/orders/" + orderId,
                                List.of(new UcpOrderLineItemDto(
                                                "line_1",
                                                new UcpItemResponse("sku-1", "Product 1", 1000L, null),
                                                new UcpOrderQuantityDto(1, 1, 1),
                                                List.of(UcpTotalResponse.total(1000L)),
                                                "fulfilled")),
                                new UcpOrderFulfillmentDto(
                                                List.of(new UcpExpectationDto(
                                                                "exp_1",
                                                                List.of(new UcpExpectationLineItemDto("line_1", 1)),
                                                                "shipping",
                                                                new UcpPostalAddressDto(null, null, null, null, null,
                                                                                null, null, null, null),
                                                                "Standard Delivery",
                                                                "now")),
                                                null),
                                "USD",
                                List.of(UcpTotalResponse.total(1000L)),
                                null,
                                null,
                                null,
                                null);
        }

        @Test
        void getOrderReturns200WithConformingOrder() throws Exception {
                String orderId = "ord_100";
                String userId = "test-user";
                when(orderService.getOrder(eq(orderId), eq(userId))).thenReturn(sampleResponse(orderId));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userId, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

                mockMvc.perform(get("/ucp/v1/orders/{id}", orderId)
                                .principal(auth)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(orderId))
                                .andExpect(jsonPath("$.checkout_id").value("chk_" + orderId))
                                .andExpect(jsonPath("$.currency").value("USD"))
                                .andExpect(jsonPath("$.line_items[0].id").value("line_1"))
                                .andExpect(jsonPath("$.line_items[0].status").value("fulfilled"))
                                .andExpect(jsonPath("$.fulfillment.expectations[0].id").value("exp_1"));
        }

        @Test
        void getOrderReturns404WhenOrderNotFound() throws Exception {
                String orderId = "ord_not_found";
                String userId = "test-user";
                when(orderService.getOrder(eq(orderId), eq(userId)))
                                .thenThrow(new UcpProtocolException(404, "item_not_found", "Order not found", "error",
                                                "$.id"));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userId, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

                mockMvc.perform(get("/ucp/v1/orders/{id}", orderId)
                                .principal(auth)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.ucp.status").value("error"))
                                .andExpect(jsonPath("$.messages[0].code").value("item_not_found"));
        }

        @Test
        void getOrderReturns403WhenForbidden() throws Exception {
                String orderId = "ord_other_user";
                String userId = "attacker";
                when(orderService.getOrder(eq(orderId), eq(userId)))
                                .thenThrow(new UcpProtocolException(403, "forbidden", "Access denied", "error",
                                                "$.id"));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userId, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

                mockMvc.perform(get("/ucp/v1/orders/{id}", orderId)
                                .principal(auth)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.ucp.status").value("error"))
                                .andExpect(jsonPath("$.messages[0].code").value("forbidden"));
        }
}
