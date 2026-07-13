package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.reservation.ReleaseReservationRequest;
import com.aionn.inventory.adapter.rest.dto.reservation.ReserveStockRequest;
import com.aionn.inventory.adapter.rest.exception.InventoryExceptionHandler;
import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.service.StockReservationService;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
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

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StockReservationControllerWebTest {

    @Mock
    private StockReservationService reservationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        StockReservationController controller = new StockReservationController(reservationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new InventoryExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void reserveReturnsCreatedWithReservationId() throws Exception {
        ReservationResult result = sampleResult("R_1", "RESERVED");
        when(reservationService.reserve(any(ReserveStockCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReserveStockRequest("SKU_1", "WH_1", "ORDER_1", 5, 60))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reservationId").value("R_1"))
                .andExpect(jsonPath("$.data.status").value("RESERVED"));

        verify(reservationService).reserve(any(ReserveStockCommand.class));
    }

    @Test
    void commitReturnsOkWithCommittedStatus() throws Exception {
        when(reservationService.commit(any(CommitReservationCommand.class)))
                .thenReturn(sampleResult("R_1", "COMMITTED"));

        mockMvc.perform(post("/api/v1/inventory/reservations/R_1/commit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMMITTED"));

        verify(reservationService).commit(any(CommitReservationCommand.class));
    }

    @Test
    void releaseReturnsOkWithReleasedStatus() throws Exception {
        when(reservationService.release(any(ReleaseReservationCommand.class)))
                .thenReturn(sampleResult("R_1", "RELEASED"));

        mockMvc.perform(post("/api/v1/inventory/reservations/R_1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReleaseReservationRequest("cancel"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RELEASED"));

        verify(reservationService).release(any(ReleaseReservationCommand.class));
    }

    @Test
    void getReturnsReservationByIdWhenFound() throws Exception {
        when(reservationService.get("R_1")).thenReturn(sampleResult("R_1", "RESERVED"));

        mockMvc.perform(get("/api/v1/inventory/reservations/R_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationId").value("R_1"));
    }

    @Test
    void getReturnsNotFoundWhenReservationMissing() throws Exception {
        when(reservationService.get("R_X"))
                .thenThrow(new InventoryException(InventoryErrorCode.STOCK_RESERVATION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/inventory/reservations/R_X"))
                .andExpect(status().isNotFound());
    }

    private ReservationResult sampleResult(String id, String status) {
        Instant now = Instant.now();
        return new ReservationResult(id, "SKU_1", "WH_1", "ORDER_1", 5, status, now,
                now.plus(Duration.ofMinutes(15)), null);
    }
}
