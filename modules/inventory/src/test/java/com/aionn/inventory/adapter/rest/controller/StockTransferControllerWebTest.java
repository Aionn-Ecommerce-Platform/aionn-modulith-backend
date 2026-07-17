package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.transfer.request.CancelTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.request.CompleteTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.request.InitiateTransferRequest;
import com.aionn.inventory.adapter.rest.exception.InventoryExceptionHandler;
import com.aionn.inventory.adapter.rest.mapper.transfer.StockTransferDtoMapperImpl;
import com.aionn.inventory.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.inventory.adapter.rest.support.TestAuth;
import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.port.in.transfer.CancelTransferInputPort;
import com.aionn.inventory.application.port.in.transfer.CompleteTransferInputPort;
import com.aionn.inventory.application.port.in.transfer.GetTransferInputPort;
import com.aionn.inventory.application.port.in.transfer.InitiateTransferInputPort;
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

    @Mock private InitiateTransferInputPort initiateTransferInputPort;
    @Mock private CompleteTransferInputPort completeTransferInputPort;
    @Mock private CancelTransferInputPort cancelTransferInputPort;
    @Mock private GetTransferInputPort getTransferInputPort;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        StockTransferController controller = new StockTransferController(
                initiateTransferInputPort,
                completeTransferInputPort,
                cancelTransferInputPort,
                getTransferInputPort,
                new StockTransferDtoMapperImpl()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new InventoryExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addInterceptors(new MockSecurityInterceptor())
                .build();
    }

    @Test
    void initiateReturnsCreatedWithTransferDetails() throws Exception {
        StockTransferResult result = sample("T_1", "INITIATED");
        when(initiateTransferInputPort.execute(any(InitiateTransferCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/inventory/transfers")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InitiateTransferRequest("WH_FROM", "WH_TO", "SKU_1", 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transferId").value("T_1"))
                .andExpect(jsonPath("$.data.status").value("INITIATED"));

        verify(initiateTransferInputPort).execute(any(InitiateTransferCommand.class));
    }

    @Test
    void completeReturnsOkWithCompletedStatus() throws Exception {
        when(completeTransferInputPort.execute(any(CompleteTransferCommand.class)))
                .thenReturn(sample("T_1", "COMPLETED"));

        mockMvc.perform(post("/api/v1/inventory/transfers/T_1/complete")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompleteTransferRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(completeTransferInputPort).execute(any(CompleteTransferCommand.class));
    }

    @Test
    void cancelReturnsOkWithCancelledStatus() throws Exception {
        when(cancelTransferInputPort.execute(any(CancelTransferCommand.class)))
                .thenReturn(sample("T_1", "CANCELLED"));

        mockMvc.perform(post("/api/v1/inventory/transfers/T_1/cancel")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelTransferRequest("damage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(cancelTransferInputPort).execute(any(CancelTransferCommand.class));
    }

    @Test
    void getReturnsTransfer() throws Exception {
        when(getTransferInputPort.execute("T_1")).thenReturn(sample("T_1", "INITIATED"));

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
