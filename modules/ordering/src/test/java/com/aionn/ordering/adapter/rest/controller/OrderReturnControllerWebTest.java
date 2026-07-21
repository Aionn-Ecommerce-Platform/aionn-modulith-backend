package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.response.OrderReturnResponse;
import com.aionn.ordering.adapter.rest.exception.OrderingExceptionHandler;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand;
import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderReturnControllerWebTest {

    @Mock private RequestReturnInputPort requestReturnInputPort;
    @Mock private ApproveReturnInputPort approveReturnInputPort;
    @Mock private RejectReturnInputPort rejectReturnInputPort;
    @Mock private ConfirmItemReceivedInputPort confirmItemReceivedInputPort;
    @Mock private GetReturnInputPort getReturnInputPort;
    @Mock private ListReturnsInputPort listReturnsInputPort;
    @Mock private OrderingDtoMapper dtoMapper;
    @Mock private com.aionn.sharedkernel.integration.port.catalog.MerchantOwnershipVerifierPort merchantOwnershipVerifierPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderReturnController controller = new OrderReturnController(
                requestReturnInputPort, approveReturnInputPort, rejectReturnInputPort,
                confirmItemReceivedInputPort, getReturnInputPort, listReturnsInputPort, dtoMapper);
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

        lenient().when(dtoMapper.toRequestReturnCommand(any(), any(), any())).thenReturn(new RequestReturnCommand("order-1", "user-1", "DEFECT", "url"));
        lenient().when(dtoMapper.toApproveReturnCommand(any(), any(), any())).thenReturn(new ApproveReturnCommand("return-1", "merchant-1", BigDecimal.TEN, "VND", "wh-1"));
        lenient().when(dtoMapper.toRejectReturnCommand(any(), any(), any())).thenReturn(new RejectReturnCommand("return-1", "merchant-1", "Reject note"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static ReturnResult sampleReturn() {
        return new ReturnResult("ret-1", "order-1", "user-1", "merch-1", "damaged",
                "http://ev", BigDecimal.valueOf(100), "VND", "wh-1", "NEW",
                null, "REQUESTED", Instant.now(), null, null);
    }

    private static OrderReturnResponse sampleResponse() {
        return new OrderReturnResponse("ret-1", "order-1", "user-1", "merch-1", "damaged",
                "http://ev", BigDecimal.valueOf(100), "VND", "wh-1", "NEW",
                null, "REQUESTED", Instant.now(), null, null);
    }

    @Test
    void requestReturnReturnsCreated() throws Exception {
        ReturnResult result = sampleReturn();
        when(requestReturnInputPort.execute(any(RequestReturnCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/ordering/returns/orders/order-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"damaged\",\"evidenceUrl\":\"http://ev\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("201"))
                .andExpect(jsonPath("$.data.returnId").value("ret-1"));
    }

    @Test
    void requestReturnFailsValidation() throws Exception {
        mockMvc.perform(post("/api/v1/ordering/returns/orders/order-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"\",\"evidenceUrl\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveReturnsSuccess() throws Exception {
        ReturnResult result = sampleReturn();
        when(approveReturnInputPort.execute(any(ApproveReturnCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/ordering/returns/ret-1/approve")
                        .header("X-Merchant-Id", "M_1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refundAmount\":100,\"currency\":\"VND\",\"returnWarehouseId\":\"wh-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectReturnsSuccess() throws Exception {
        ReturnResult result = sampleReturn();
        when(rejectReturnInputPort.execute(any(RejectReturnCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/ordering/returns/ret-1/reject")
                        .header("X-Merchant-Id", "M_1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"not-eligible\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void confirmReceivedReturnsSuccess() throws Exception {
        ReturnResult result = sampleReturn();
        when(confirmItemReceivedInputPort.execute(any(ConfirmItemReceivedCommand.class))).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/ordering/returns/ret-1/item-received")
                        .header("X-Merchant-Id", "M_1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"itemCondition\":\"GOOD\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void listMineReturnsList() throws Exception {
        ReturnResult result = sampleReturn();
        when(listReturnsInputPort.execute("user-1", "USER", 50)).thenReturn(List.of(result));
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/ordering/returns/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].returnId").value("ret-1"));
    }

    @Test
    void listMerchantReturnsList() throws Exception {
        ReturnResult result = sampleReturn();
        when(listReturnsInputPort.execute("M_1", "MERCHANT", 50)).thenReturn(List.of(result));
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/ordering/returns/merchant")
                        .header("X-Merchant-Id", "M_1"))
                .andExpect(status().isOk());
    }

    @Test
    void getReturnReturnsItem() throws Exception {
        ReturnResult result = sampleReturn();
        when(getReturnInputPort.execute("ret-1", "user-1")).thenReturn(result);
        when(dtoMapper.toResponse(result)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/ordering/returns/ret-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnId").value("ret-1"));
    }
}
