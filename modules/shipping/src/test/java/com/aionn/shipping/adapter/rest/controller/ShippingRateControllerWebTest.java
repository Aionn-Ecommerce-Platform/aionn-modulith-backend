package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.rate.ConfigureRateRequest;
import com.aionn.shipping.adapter.rest.dto.rate.UpdateRateRequest;
import com.aionn.shipping.adapter.rest.exception.ShippingExceptionHandler;
import com.aionn.shipping.adapter.rest.mapper.rate.ShippingRateDtoMapper;
import com.aionn.shipping.application.dto.rate.command.ConfigureRateCommand;
import com.aionn.shipping.application.dto.rate.command.UpdateRateCommand;
import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;
import com.aionn.shipping.application.port.in.rate.ConfigureRateInputPort;
import com.aionn.shipping.application.port.in.rate.UpdateRateInputPort;
import com.aionn.shipping.application.port.in.rate.GetRateInputPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShippingRateControllerWebTest {

    @Mock
    private ConfigureRateInputPort configureRateInputPort;
    @Mock
    private UpdateRateInputPort updateRateInputPort;
    @Mock
    private GetRateInputPort getRateInputPort;

    private final ShippingRateDtoMapper shippingRateDtoMapper = Mappers.getMapper(ShippingRateDtoMapper.class);

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        ShippingRateController controller = new ShippingRateController(
                configureRateInputPort,
                updateRateInputPort,
                getRateInputPort,
                shippingRateDtoMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ShippingExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void configureReturnsCreatedWithRate() throws Exception {
        when(configureRateInputPort.execute(any(ConfigureRateCommand.class)))
                .thenReturn(sample("R_1", "HN", BigDecimal.valueOf(30000)));

        mockMvc.perform(post("/api/v1/shipping/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfigureRateRequest(
                                "HN", BigDecimal.valueOf(30000), "VND", "<=2kg"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rateId").value("R_1"))
                .andExpect(jsonPath("$.data.zoneCode").value("HN"))
                .andExpect(jsonPath("$.data.baseFee").value(30000));

        verify(configureRateInputPort).execute(any(ConfigureRateCommand.class));
    }

    @Test
    void configureReturnsConflictWhenDuplicateZone() throws Exception {
        when(configureRateInputPort.execute(any(ConfigureRateCommand.class)))
                .thenThrow(new ShippingException(ShippingErrorCode.RATE_DUPLICATE));

        mockMvc.perform(post("/api/v1/shipping/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfigureRateRequest(
                                "HN", BigDecimal.valueOf(30000), "VND", "<=2kg"))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateReturnsOkWithUpdatedFee() throws Exception {
        when(updateRateInputPort.execute(any(UpdateRateCommand.class)))
                .thenReturn(sample("R_1", "HN", BigDecimal.valueOf(50000)));

        mockMvc.perform(put("/api/v1/shipping/rates/R_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRateRequest(
                                BigDecimal.valueOf(50000), "<=5kg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseFee").value(50000));

        verify(updateRateInputPort).execute(any(UpdateRateCommand.class));
    }

    @Test
    void getReturnsRateById() throws Exception {
        when(getRateInputPort.execute("R_1")).thenReturn(sample("R_1", "HN", BigDecimal.valueOf(30000)));

        mockMvc.perform(get("/api/v1/shipping/rates/R_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rateId").value("R_1"));
    }

    @Test
    void getReturnsNotFoundWhenRateMissing() throws Exception {
        when(getRateInputPort.execute("R_X"))
                .thenThrow(new ShippingException(ShippingErrorCode.RATE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/shipping/rates/R_X"))
                .andExpect(status().isNotFound());
    }

    private ShippingRateResult sample(String id, String zone, BigDecimal fee) {
        Instant now = Instant.now();
        return new ShippingRateResult(id, zone, fee, "VND", "<=2kg", now, now);
    }
}
