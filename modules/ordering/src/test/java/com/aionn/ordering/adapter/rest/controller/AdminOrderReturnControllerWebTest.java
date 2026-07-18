package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.response.OrderReturnResponse;
import com.aionn.ordering.adapter.rest.exception.OrderingExceptionHandler;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.service.OrderReturnService;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.sharedkernel.adapter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderReturnControllerWebTest {

    @Mock private OrderReturnService returnService;
    @Mock private OrderingDtoMapper dtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminOrderReturnController controller = new AdminOrderReturnController(returnService, dtoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new OrderingExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .build();
    }

    private static ReturnResult sampleResult() {
        return new ReturnResult("ret-1", "order-1", "user-1", "merch-1", "broken", null, null, null, null, null, null, "REQUESTED", Instant.now(), null, null);
    }

    private static OrderReturnResponse sampleResponse() {
        return new OrderReturnResponse("ret-1", "order-1", "user-1", "merch-1", "broken", null, null, null, null, null, null, "REQUESTED", Instant.now(), null, null);
    }

    @Test
    void listByStatusReturnsSuccess() throws Exception {
        ReturnResult result = sampleResult();
        when(returnService.adminListByStatus(ReturnStatus.REQUESTED, 50)).thenReturn(List.of(result));
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/admin/ordering/returns"))
                .andExpect(status().isOk());
    }

    @Test
    void analyticsReturnsSuccess() throws Exception {
        var result = new com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult(
                java.time.LocalDate.now().minusDays(29), java.time.LocalDate.now(),
                0, 0, 0.0, BigDecimal.ZERO, "VND", List.of(), List.of()
        );
        when(returnService.adminAnalytics(any(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/admin/ordering/returns/analytics"))
                .andExpect(status().isOk());
    }

    @Test
    void getReturnReturnsSuccess() throws Exception {
        ReturnResult result = sampleResult();
        when(returnService.adminGet("ret-1")).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/admin/ordering/returns/ret-1"))
                .andExpect(status().isOk());
    }

    @Test
    void approveReturnsSuccess() throws Exception {
        ReturnResult result = sampleResult();
        when(returnService.adminApprove(any(), any(), any(), any())).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        String json = "{\"refundAmount\":100,\"currency\":\"VND\",\"returnWarehouseId\":\"wh-1\"}";
        mockMvc.perform(post("/api/v1/admin/ordering/returns/ret-1/approve")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void rejectReturnsSuccess() throws Exception {
        ReturnResult result = sampleResult();
        when(returnService.adminReject("ret-1", "invalid")).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        String json = "{\"reason\":\"invalid\"}";
        mockMvc.perform(post("/api/v1/admin/ordering/returns/ret-1/reject")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void confirmReceivedReturnsSuccess() throws Exception {
        ReturnResult result = sampleResult();
        when(returnService.adminConfirmItemReceived("ret-1", "GOOD")).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        String json = "{\"itemCondition\":\"GOOD\"}";
        mockMvc.perform(post("/api/v1/admin/ordering/returns/ret-1/item-received")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}