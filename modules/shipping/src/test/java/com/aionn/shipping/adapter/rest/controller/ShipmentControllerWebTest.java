package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.shipment.CancelShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.CreateShipmentRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.QuoteShippingRequest;
import com.aionn.shipping.adapter.rest.exception.ShippingExceptionHandler;
import com.aionn.shipping.adapter.rest.support.session.CurrentUserId;
import com.aionn.shipping.adapter.rest.mapper.shipment.ShipmentDtoMapper;
import com.aionn.shipping.adapter.rest.mapper.rate.ShippingRateDtoMapper;
import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.QuoteShippingInputPort;
import com.aionn.shipping.application.port.in.shipment.CreateShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.FetchLabelInputPort;
import com.aionn.shipping.application.port.in.shipment.CancelShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.ResolveIssueInputPort;
import com.aionn.shipping.application.port.in.shipment.GetShipmentInputPort;
import com.aionn.shipping.application.port.in.shipment.ListShipmentsByOrderInputPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShipmentControllerWebTest {

    @Mock
    private QuoteShippingInputPort quoteShippingInputPort;
    @Mock
    private CreateShipmentInputPort createShipmentInputPort;
    @Mock
    private RegisterShipmentInputPort registerShipmentInputPort;
    @Mock
    private FetchLabelInputPort fetchLabelInputPort;
    @Mock
    private CancelShipmentInputPort cancelShipmentInputPort;
    @Mock
    private ResolveIssueInputPort resolveIssueInputPort;
    @Mock
    private GetShipmentInputPort getShipmentInputPort;
    @Mock
    private ListShipmentsByOrderInputPort listShipmentsByOrderInputPort;

    private final ShipmentDtoMapper shipmentDtoMapper = Mappers.getMapper(ShipmentDtoMapper.class);
    private final ShippingRateDtoMapper shippingRateDtoMapper = Mappers.getMapper(ShippingRateDtoMapper.class);

    private MockMvc mockMvc;
    private final JsonMapper objectMapper = JacksonMapperFactory.create();

    @BeforeEach
    void setUp() {
        ShipmentController controller = new ShipmentController(
                quoteShippingInputPort,
                createShipmentInputPort,
                registerShipmentInputPort,
                fetchLabelInputPort,
                cancelShipmentInputPort,
                resolveIssueInputPort,
                getShipmentInputPort,
                listShipmentsByOrderInputPort,
                shipmentDtoMapper,
                shippingRateDtoMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ShippingExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new StubCurrentUserIdResolver("user-1"))
                .build();
    }

    @Test
    void quoteReturnsQuoteResult() throws Exception {
        ShippingQuoteResult quote = new ShippingQuoteResult(
                BigDecimal.valueOf(30000), "VND", "HN", "configured-rate",
                "<=2kg", null, null);
        when(quoteShippingInputPort.execute(any(QuoteShippingCommand.class))).thenReturn(quote);

        ShipmentAddress address = new ShipmentAddress(
                "John", "0912345678", "addr", "00001", "001", "HN", "VN");
        ShipmentDimensions dims = new ShipmentDimensions(
                500, BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(10));

        mockMvc.perform(post("/api/v1/shipping/shipments/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new QuoteShippingRequest(null, address, dims, "VND"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee").value(30000))
                .andExpect(jsonPath("$.data.currency").value("VND"));

        ArgumentCaptor<QuoteShippingCommand> commandCaptor = ArgumentCaptor.forClass(QuoteShippingCommand.class);
        verify(quoteShippingInputPort).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue().orderId()).isNull();
    }

    @Test
    void createReturnsCreatedWhenMerchantResolved() throws Exception {
        when(createShipmentInputPort.execute(any(CreateShipmentCommand.class)))
                .thenReturn(sample("S_1", "REQUESTED"));

        ShipmentAddress address = new ShipmentAddress(
                "John", "0912345678", "addr", "00001", "001", "HN", "VN");
        ShipmentDimensions dims = new ShipmentDimensions(
                500, BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(10));

        mockMvc.perform(post("/api/v1/shipping/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShipmentRequest(
                                "ORDER_1", "U_BUYER", address, dims,
                                BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shipmentId").value("S_1"));

        verify(createShipmentInputPort).execute(any(CreateShipmentCommand.class));
    }

    @Test
    void createReturnsForbiddenWhenOwnerHasNoMerchant() throws Exception {
        when(createShipmentInputPort.execute(any(CreateShipmentCommand.class)))
                .thenThrow(new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN));

        ShipmentAddress address = new ShipmentAddress(
                "John", "0912345678", "addr", "00001", "001", "HN", "VN");
        ShipmentDimensions dims = new ShipmentDimensions(
                500, BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(10));

        mockMvc.perform(post("/api/v1/shipping/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShipmentRequest(
                                "ORDER_1", "U_BUYER", address, dims,
                                BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelReturnsOkWithCancelledStatus() throws Exception {
        when(cancelShipmentInputPort.execute(any(CancelShipmentCommand.class)))
                .thenReturn(sample("S_1", "CANCELLED"));

        mockMvc.perform(post("/api/v1/shipping/shipments/S_1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelShipmentRequest("buyer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void getReturnsShipmentByIdForViewer() throws Exception {
        when(getShipmentInputPort.execute("S_1", "user-1")).thenReturn(sample("S_1", "REGISTERED"));

        mockMvc.perform(get("/api/v1/shipping/shipments/S_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentId").value("S_1"));
    }

    @Test
    void getReturnsNotFoundWhenShipmentMissing() throws Exception {
        when(getShipmentInputPort.execute("S_X", "user-1"))
                .thenThrow(new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/shipping/shipments/S_X"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerWithCarrierReturnsRegisteredShipment() throws Exception {
        when(registerShipmentInputPort.execute("S_1")).thenReturn(sample("S_1", "REGISTERED"));

        mockMvc.perform(post("/api/v1/shipping/shipments/S_1/register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    @Test
    void fetchLabelReturnsLabelFetchedShipment() throws Exception {
        when(fetchLabelInputPort.execute(any())).thenReturn(sample("S_1", "REGISTERED"));

        mockMvc.perform(post("/api/v1/shipping/shipments/S_1/label"))
                .andExpect(status().isOk());
    }

    @Test
    void resolveIssueReturnsUpdatedShipment() throws Exception {
        when(resolveIssueInputPort.execute(any())).thenReturn(sample("S_1", "REGISTERED"));

        mockMvc.perform(post("/api/v1/shipping/shipments/S_1/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueType\":\"DELAY\",\"resolution\":\"fixed\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void listByOrderIdReturnsShipmentsList() throws Exception {
        when(listShipmentsByOrderInputPort.execute("ORDER_1", "user-1")).thenReturn(List.of(sample("S_1", "REGISTERED")));

        mockMvc.perform(get("/api/v1/shipping/shipments/by-order/ORDER_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shipmentId").value("S_1"));
    }

    private ShipmentResult sample(String id, String status) {
        Instant now = Instant.now();
        return new ShipmentResult(id, "ORDER_1", "M_1", "U_1",
                null, null, null, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND",
                status, null, null, null, 0, null, null, null, null, null, null, now, now);
    }

    private static class StubCurrentUserIdResolver implements HandlerMethodArgumentResolver {
        private final String userId;

        StubCurrentUserIdResolver(String userId) {
            this.userId = userId;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUserId.class)
                    && String.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return userId;
        }
    }
}
