package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.transfer.CancelTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.CompleteTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.InitiateTransferRequest;
import com.aionn.inventory.adapter.rest.exception.InventoryExceptionHandler;
import com.aionn.inventory.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.inventory.adapter.rest.support.TestAuth;
import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.service.StockTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StockTransferControllerWebTest {

    @Mock
    private StockTransferService transferService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        StockTransferController controller = new StockTransferController(transferService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new InventoryExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addInterceptors(new MockSecurityInterceptor())
                .build();
    }

    @Test
    void initiateReturnsCreatedWithTransferDetails() throws Exception {
        StockTransferResult result = sample("T_1", "INITIATED");
        when(transferService.initiate(any(InitiateTransferCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/inventory/transfers")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InitiateTransferRequest("WH_FROM", "WH_TO", "SKU_1", 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transferId").value("T_1"))
                .andExpect(jsonPath("$.data.status").value("INITIATED"));

        verify(transferService).initiate(any(InitiateTransferCommand.class));
    }

    @Test
    void completeReturnsOkWithCompletedStatus() throws Exception {
        when(transferService.complete(any(CompleteTransferCommand.class)))
                .thenReturn(sample("T_1", "COMPLETED"));

        mockMvc.perform(post("/api/v1/inventory/transfers/T_1/complete")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompleteTransferRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(transferService).complete(any(CompleteTransferCommand.class));
    }

    @Test
    void cancelReturnsOkWithCancelledStatus() throws Exception {
        when(transferService.cancel(any(CancelTransferCommand.class)))
                .thenReturn(sample("T_1", "CANCELLED"));

        mockMvc.perform(post("/api/v1/inventory/transfers/T_1/cancel")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelTransferRequest("damage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(transferService).cancel(any(CancelTransferCommand.class));
    }

    @Test
    void getReturnsTransfer() throws Exception {
        when(transferService.get("T_1")).thenReturn(sample("T_1", "INITIATED"));

        mockMvc.perform(get("/api/v1/inventory/transfers/T_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferId").value("T_1"));
    }

    private StockTransferResult sample(String id, String status) {
        Instant now = Instant.now();
        return new StockTransferResult(id, "M_1", "WH_FROM", "WH_TO", "SKU_1", 5, status,
                now, null, null);
    }
}
