package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.response.OrderResponse;
import com.aionn.ordering.adapter.rest.exception.OrderingExceptionHandler;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.ordering.application.dto.order.command.*;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.port.in.order.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerWebTest {

    @Mock private PlaceOrderInputPort placeOrderInputPort;
    @Mock private ConfirmPreparationInputPort confirmPreparationInputPort;
    @Mock private CancelOrderInputPort cancelOrderInputPort;
    @Mock private RejectOrderInputPort rejectOrderInputPort;
    @Mock private ChangeShippingInfoInputPort changeShippingInfoInputPort;
    @Mock private ConfirmShippedInputPort confirmShippedInputPort;
    @Mock private ConfirmDeliveredInputPort confirmDeliveredInputPort;
    @Mock private GetOrderInputPort getOrderInputPort;
    @Mock private ListOrdersInputPort listOrdersInputPort;
    @Mock private GetMerchantOrderAnalyticsInputPort getMerchantOrderAnalyticsInputPort;
    @Mock private GetPlatformOrderAnalyticsInputPort getPlatformOrderAnalyticsInputPort;
    @Mock private GetTopProductsInputPort getTopProductsInputPort;
    @Mock private OrderingDtoMapper dtoMapper;
    @Mock private com.aionn.sharedkernel.integration.port.catalog.MerchantOwnershipVerifierPort merchantOwnershipVerifierPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(
                placeOrderInputPort, confirmPreparationInputPort, cancelOrderInputPort, rejectOrderInputPort,
                changeShippingInfoInputPort, confirmShippedInputPort, confirmDeliveredInputPort,
                getOrderInputPort, listOrdersInputPort, getMerchantOrderAnalyticsInputPort,
                getPlatformOrderAnalyticsInputPort, getTopProductsInputPort, dtoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new OrderingExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver(),
                        new com.aionn.ordering.adapter.rest.support.session.CurrentMerchantIdArgumentResolver(merchantOwnershipVerifierPort))
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user-1", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        org.mockito.Mockito.lenient().when(merchantOwnershipVerifierPort.isOwnedBy(any(), any())).thenReturn(true);

        lenient().when(dtoMapper.toPlaceOrderCommand(any(), any())).thenReturn(new PlaceOrderCommand("user-1", "addr-1", "COD", "VND", BigDecimal.ZERO, null, List.of(), "COD"));
        lenient().when(dtoMapper.toConfirmPreparationCommand(any(), any())).thenReturn(new ConfirmPreparationCommand("order-1", "merchant-1"));
        lenient().when(dtoMapper.toCancelOrderCommand(any(), any(), any())).thenReturn(new CancelOrderCommand("order-1", "user-1", "Reason"));
        lenient().when(dtoMapper.toRejectOrderCommand(any(), any(), any())).thenReturn(new RejectOrderCommand("order-1", "merchant-1", "Reason"));
        lenient().when(dtoMapper.toChangeShippingInfoCommand(any(), any(), any())).thenReturn(new ChangeShippingInfoCommand("order-1", "user-1", null, BigDecimal.ZERO));
        lenient().when(dtoMapper.toConfirmShippedCommand(any(), any())).thenReturn(new ConfirmShippedCommand("order-1", "ship-1"));
        lenient().when(dtoMapper.toConfirmDeliveredCommand(any())).thenReturn(new ConfirmDeliveredCommand("order-1"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static OrderResult sampleResult(String status) {
        return new OrderResult("order-1", null, "user-1", "merchant-1", "prop-1",
                "COD", null, "VND", BigDecimal.valueOf(200),
                BigDecimal.ZERO, "addr-1", "John", "12345", "line1",
                "ward", "dist", "prov", "VN", List.of(), status, null,
                Instant.now(), Instant.now(), null, null);
    }

    private static OrderResponse sampleResponse(String status) {
        return new OrderResponse("order-1", null, "user-1", "merchant-1", "prop-1",
                "COD", null, "VND", BigDecimal.valueOf(200),
                BigDecimal.ZERO, "addr-1", "John", "12345", "line1",
                "ward", "dist", "prov", "VN", List.of(), status, null,
                Instant.now(), Instant.now(), null, null);
    }

    @Test
    void placeOrderReturnsCreated() throws Exception {
        OrderResult result = sampleResult("APPROVED");
        when(placeOrderInputPort.execute(any(PlaceOrderCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("APPROVED"));

        mockMvc.perform(post("/api/v1/ordering/orders")
                        .contentType(APPLICATION_JSON)
                        .content("{\"addressId\":\"addr-1\",\"paymentMethodId\":\"COD\",\"currency\":\"VND\",\"shippingFee\":0,\"selectedSkuIds\":[],\"gateway\":\"COD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("201"))
                .andExpect(jsonPath("$.data.orderId").value("order-1"));
    }

    @Test
    void confirmPreparationReturnsSuccess() throws Exception {
        OrderResult result = sampleResult("PREPARING");
        when(confirmPreparationInputPort.execute(any(ConfirmPreparationCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("PREPARING"));

        mockMvc.perform(post("/api/v1/ordering/orders/order-1/confirm-preparation")
                        .header("X-Merchant-Id", "M_1")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelReturnsSuccess() throws Exception {
        OrderResult result = sampleResult("CANCELLED");
        when(cancelOrderInputPort.execute(any(CancelOrderCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("CANCELLED"));

        mockMvc.perform(post("/api/v1/ordering/orders/order-1/cancel")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"changed\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectReturnsSuccess() throws Exception {
        OrderResult result = sampleResult("REJECTED");
        when(rejectOrderInputPort.execute(any(RejectOrderCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("REJECTED"));

        mockMvc.perform(post("/api/v1/ordering/orders/order-1/reject")
                        .header("X-Merchant-Id", "M_1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"out-of-stock\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changeShippingInfoReturnsSuccess() throws Exception {
        OrderResult result = sampleResult("PENDING");
        when(changeShippingInfoInputPort.execute(any(ChangeShippingInfoCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("PENDING"));

        mockMvc.perform(put("/api/v1/ordering/orders/order-1/shipping-info")
                        .contentType(APPLICATION_JSON)
                        .content("{\"newAddress\":{\"addressId\":\"addr-2\",\"recipientName\":\"Max\"},\"newShippingFee\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void shipReturnsSuccess() throws Exception {
        OrderResult result = sampleResult("SHIPPED");
        when(confirmShippedInputPort.execute(any(ConfirmShippedCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("SHIPPED"));

        mockMvc.perform(post("/api/v1/ordering/orders/order-1/ship")
                        .contentType(APPLICATION_JSON)
                        .content("{\"shipmentId\":\"ship-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void completeReturnsSuccess() throws Exception {
        OrderResult result = sampleResult("COMPLETED");
        when(confirmDeliveredInputPort.execute(any(ConfirmDeliveredCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("COMPLETED"));

        mockMvc.perform(post("/api/v1/ordering/orders/order-1/complete"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrderReturnsSuccess() throws Exception {
        OrderResult result = sampleResult("APPROVED");
        when(getOrderInputPort.execute("order-1", "user-1")).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("APPROVED"));

        mockMvc.perform(get("/api/v1/ordering/orders/order-1"))
                .andExpect(status().isOk());
    }

    @Test
    void listMineReturnsOrders() throws Exception {
        OrderResult result = sampleResult("PENDING");
        when(listOrdersInputPort.execute("user-1", "USER", null, 20)).thenReturn(List.of(result));
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("PENDING"));

        mockMvc.perform(get("/api/v1/ordering/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void listForMerchantReturnsOrders() throws Exception {
        OrderResult result = sampleResult("PENDING");
        when(listOrdersInputPort.execute(any(), eq("MERCHANT"), any(), any(int.class)))
                .thenReturn(List.of(result));
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse("PENDING"));

        mockMvc.perform(get("/api/v1/ordering/orders/merchant").header("X-Merchant-Id", "merchant-1"))
                .andExpect(status().isOk());
    }

    @Test
    void merchantAnalyticsReturnsResult() throws Exception {
        var analytics = new com.aionn.ordering.application.dto.order.result.MerchantOrderAnalyticsResult(
                java.time.LocalDate.now().minusDays(6), java.time.LocalDate.now(),
                "VND", java.math.BigDecimal.ZERO, 0L, 0L, List.of(), List.of());
        when(getMerchantOrderAnalyticsInputPort.execute(any(), any(), any())).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/ordering/orders/merchant/analytics").header("X-Merchant-Id", "merchant-1"))
                .andExpect(status().isOk());
    }

    @Test
    void merchantTopProductsReturnsResult() throws Exception {
        when(getTopProductsInputPort.execute(any(), any(), any(), any(int.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ordering/orders/merchant/top-products").header("X-Merchant-Id", "merchant-1"))
                .andExpect(status().isOk());
    }

    @Test
    void platformAnalyticsReturnsResult() throws Exception {
        var analytics = new com.aionn.ordering.application.dto.order.result.PlatformOrderAnalyticsResult(
                java.time.LocalDate.now().minusDays(6), java.time.LocalDate.now(),
                "VND", java.math.BigDecimal.ZERO, 0L, 0L, List.of(), List.of(), List.of());
        when(getPlatformOrderAnalyticsInputPort.execute(any(), any())).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/ordering/orders/admin/analytics"))
                .andExpect(status().isOk());
    }
}

