package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.shipment.CarrierWebhookRequest;
import com.aionn.shipping.adapter.rest.exception.ShippingExceptionHandler;
import com.aionn.shipping.adapter.rest.mapper.shipment.ShipmentDtoMapper;
import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.ApplyCarrierWebhookInputPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShippingWebhookControllerWebTest {

    @Mock
    private ApplyCarrierWebhookInputPort applyCarrierWebhookInputPort;

    private final ShipmentDtoMapper shipmentDtoMapper = Mappers.getMapper(ShipmentDtoMapper.class);
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private MockMvc buildMockMvc() {
        ShippingWebhookController controller = new ShippingWebhookController(applyCarrierWebhookInputPort, shipmentDtoMapper);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ShippingExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private String validPayload() throws Exception {
        return objectMapper.writeValueAsString(
                new CarrierWebhookRequest(
                        "TRACK_1", "PICKED_UP", null, null, null, null, null, null, "WH_1"));
    }

    @Test
    void carrierWebhookAcceptsValidSecret() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        when(applyCarrierWebhookInputPort.execute(argThat(cmd -> "expected-secret".equals(cmd.webhookSecret()))))
                .thenReturn(sample("S_1", "PICKED_UP"));

        mockMvc.perform(post("/api/v1/shipping/webhooks/carrier")
                        .header("X-Webhook-Secret", "expected-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentId").value("S_1"));

        verify(applyCarrierWebhookInputPort).execute(any(CarrierWebhookCommand.class));
    }

    @Test
    void carrierWebhookRejectsInvalidSecret() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        when(applyCarrierWebhookInputPort.execute(argThat(cmd -> "WRONG".equals(cmd.webhookSecret()))))
                .thenThrow(new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));

        mockMvc.perform(post("/api/v1/shipping/webhooks/carrier")
                        .header("X-Webhook-Secret", "WRONG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void carrierWebhookRejectsMissingSecretWhenConfigured() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        when(applyCarrierWebhookInputPort.execute(argThat(cmd -> cmd.webhookSecret() == null)))
                .thenThrow(new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));

        mockMvc.perform(post("/api/v1/shipping/webhooks/carrier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void carrierWebhookSkipsAuthWhenSecretNotConfigured() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        when(applyCarrierWebhookInputPort.execute(argThat(cmd -> cmd.webhookSecret() == null)))
                .thenReturn(sample("S_1", "PICKED_UP"));

        mockMvc.perform(post("/api/v1/shipping/webhooks/carrier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk());

        verify(applyCarrierWebhookInputPort).execute(any(CarrierWebhookCommand.class));
    }

    @Test
    void carrierWebhookValidatesPayload() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        String invalid = objectMapper.writeValueAsString(
                new CarrierWebhookRequest(
                        " ", " ", null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/shipping/webhooks/carrier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().is4xxClientError());

        verify(applyCarrierWebhookInputPort, never()).execute(any());
    }

    private ShipmentResult sample(String id, String status) {
        Instant now = Instant.now();
        return new ShipmentResult(id, "ORDER_1", "M_1", "U_1",
                "TRACK_1", "CARRIER_1", null, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND",
                status, null, null, null, 0, null, null, null, null, null, null, now, now);
    }
}
