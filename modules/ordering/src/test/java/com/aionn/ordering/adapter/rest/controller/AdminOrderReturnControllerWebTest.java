package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.response.OrderReturnResponse;
import com.aionn.ordering.adapter.rest.exception.OrderingExceptionHandler;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.AdminApproveReturnInputPort;
import com.aionn.ordering.application.port.in.returns.AdminConfirmItemReceivedInputPort;
import com.aionn.ordering.application.port.in.returns.AdminGetReturnInputPort;
import com.aionn.ordering.application.port.in.returns.AdminListReturnsInputPort;
import com.aionn.ordering.application.port.in.returns.AdminRejectReturnInputPort;
import com.aionn.ordering.application.port.in.returns.AdminReturnAnalyticsInputPort;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.sharedkernel.adapter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
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

        @Mock
        private AdminListReturnsInputPort adminListReturnsInputPort;
        @Mock
        private AdminGetReturnInputPort adminGetReturnInputPort;
        @Mock
        private AdminReturnAnalyticsInputPort adminReturnAnalyticsInputPort;
        @Mock
        private AdminApproveReturnInputPort adminApproveReturnInputPort;
        @Mock
        private AdminRejectReturnInputPort adminRejectReturnInputPort;
        @Mock
        private AdminConfirmItemReceivedInputPort adminConfirmItemReceivedInputPort;
        @Mock
        private OrderingDtoMapper dtoMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                AdminOrderReturnController controller = new AdminOrderReturnController(
                                adminListReturnsInputPort, adminGetReturnInputPort, adminReturnAnalyticsInputPort,
                                adminApproveReturnInputPort, adminRejectReturnInputPort,
                                adminConfirmItemReceivedInputPort,
                                dtoMapper);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new OrderingExceptionHandler(), new GlobalExceptionHandler())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                                                JsonMapper.builder().build()))
                                .build();
        }

        private static ReturnResult sampleResult() {
                return new ReturnResult("ret-1", "order-1", "user-1", "merch-1", "broken", null, null, null, null, null,
                                null,
                                "REQUESTED", Instant.now(), null, null);
        }

        private static OrderReturnResponse sampleResponse() {
                return new OrderReturnResponse("ret-1", "order-1", "user-1", "merch-1", "broken", null, null, null,
                                null, null,
                                null, "REQUESTED", Instant.now(), null, null);
        }

        @Test
        void listByStatusReturnsSuccess() throws Exception {
                ReturnResult result = sampleResult();
                when(adminListReturnsInputPort.execute(ReturnStatus.REQUESTED, 50)).thenReturn(List.of(result));
                when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

                mockMvc.perform(get("/api/v1/admin/ordering/returns"))
                                .andExpect(status().isOk());
        }

        @Test
        void analyticsReturnsSuccess() throws Exception {
                var result = new com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult(
                                java.time.LocalDate.now().minusDays(29), java.time.LocalDate.now(),
                                0, 0, 0.0, BigDecimal.ZERO, "VND", List.of(), List.of());
                when(adminReturnAnalyticsInputPort.execute(any(), any())).thenReturn(result);

                mockMvc.perform(get("/api/v1/admin/ordering/returns/analytics"))
                                .andExpect(status().isOk());
        }

        @Test
        void getReturnReturnsSuccess() throws Exception {
                ReturnResult result = sampleResult();
                when(adminGetReturnInputPort.execute("ret-1")).thenReturn(result);
                when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

                mockMvc.perform(get("/api/v1/admin/ordering/returns/ret-1"))
                                .andExpect(status().isOk());
        }

        @Test
        void approveReturnsSuccess() throws Exception {
                ReturnResult result = sampleResult();
                when(adminApproveReturnInputPort.execute(any(), any(), any(), any())).thenReturn(result);
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
                when(adminRejectReturnInputPort.execute("ret-1", "invalid")).thenReturn(result);
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
                when(adminConfirmItemReceivedInputPort.execute("ret-1", "GOOD")).thenReturn(result);
                when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

                String json = "{\"itemCondition\":\"GOOD\"}";
                mockMvc.perform(post("/api/v1/admin/ordering/returns/ret-1/item-received")
                                .contentType(APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk());
        }
}
