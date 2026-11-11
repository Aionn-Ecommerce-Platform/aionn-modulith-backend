package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.mapper.method.PaymentMethodDtoMapper;
import com.aionn.payment.adapter.rest.mapper.preference.PaymentPreferenceDtoMapper;
import com.aionn.payment.application.port.in.method.CompleteStripeSetupIntentInputPort;
import com.aionn.payment.application.port.in.method.CreateStripeSetupIntentInputPort;
import com.aionn.payment.application.port.in.method.ListPaymentMethodsInputPort;
import com.aionn.payment.application.port.in.method.RemovePaymentMethodInputPort;
import com.aionn.payment.application.port.in.method.VerifyPaymentMethodInputPort;
import com.aionn.payment.application.port.in.preference.GetPaymentPreferenceInputPort;
import com.aionn.payment.application.port.in.preference.UpdatePaymentPreferenceInputPort;
import org.mockito.Mockito;

import com.aionn.payment.adapter.rest.dto.method.response.PaymentMethodResponse;
import com.aionn.payment.adapter.rest.dto.method.response.StripeSetupIntentResponse;
import com.aionn.payment.adapter.rest.dto.preference.response.PaymentPreferenceResponse;
import com.aionn.payment.adapter.rest.exception.PaymentExceptionHandler;
import com.aionn.payment.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;
import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;
import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aionn.platform.infrastructure.config.JacksonMapperFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentMethodControllerWebTest {

    @Mock
    private VerifyPaymentMethodInputPort verifyPaymentMethodInputPort;
    @Mock
    private RemovePaymentMethodInputPort removePaymentMethodInputPort;
    @Mock
    private ListPaymentMethodsInputPort listPaymentMethodsInputPort;
    @Mock
    private CreateStripeSetupIntentInputPort createStripeSetupIntentInputPort;
    @Mock
    private CompleteStripeSetupIntentInputPort completeStripeSetupIntentInputPort;
    @Mock
    private GetPaymentPreferenceInputPort getPaymentPreferenceInputPort;
    @Mock
    private UpdatePaymentPreferenceInputPort updatePaymentPreferenceInputPort;
    @Mock
    private PaymentMethodDtoMapper paymentMethodDtoMapper;
    @Mock
    private PaymentPreferenceDtoMapper paymentPreferenceDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentMethodController controller = new PaymentMethodController(
                verifyPaymentMethodInputPort,
                removePaymentMethodInputPort,
                listPaymentMethodsInputPort,
                createStripeSetupIntentInputPort,
                completeStripeSetupIntentInputPort,
                getPaymentPreferenceInputPort,
                updatePaymentPreferenceInputPort,
                paymentMethodDtoMapper,
                paymentPreferenceDtoMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JacksonMapperFactory.create()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-123", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static PaymentMethodResult methodResult(String methodId, String status) {
        Instant now = Instant.now();
        return new PaymentMethodResult(
                methodId, "user-123", "stripe", "4242", status,
                now, now, "VERIFIED".equals(status) ? now : null);
    }

    private static PaymentMethodResponse methodResponse(String methodId, String status) {
        Instant now = Instant.now();
        return new PaymentMethodResponse(
                methodId, "user-123", "stripe", "4242", status,
                now, now, "VERIFIED".equals(status) ? now : null);
    }

    @Test
    void verifyTransitionsToVerified() throws Exception {
        PaymentMethodResult result = methodResult("m-2", "VERIFIED");
        PaymentMethodResponse response = methodResponse("m-2", "VERIFIED");
        VerifyMethodCommand command = new VerifyMethodCommand("user-123", "m-2");
        Mockito.lenient().when(paymentMethodDtoMapper.toVerifyCommand("user-123", "m-2")).thenReturn(command);

        when(verifyPaymentMethodInputPort.execute(any(VerifyMethodCommand.class))).thenReturn(result);
        when(paymentMethodDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/methods/m-2/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.methodId").value("m-2"))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));

        verify(verifyPaymentMethodInputPort).execute(any(VerifyMethodCommand.class));
    }

    @Test
    void removeReturnsNoContent() throws Exception {
        RemoveMethodCommand command = new RemoveMethodCommand("user-123", "m-3");
        Mockito.lenient().when(paymentMethodDtoMapper.toRemoveCommand("user-123", "m-3")).thenReturn(command);

        mockMvc.perform(delete("/api/v1/payments/methods/m-3"))
                .andExpect(status().isNoContent());

        verify(removePaymentMethodInputPort).execute(any(RemoveMethodCommand.class));
    }

    @Test
    void listMineReturnsActiveMethods() throws Exception {
        PaymentMethodResult r1 = methodResult("m-1", "VERIFIED");
        PaymentMethodResult r2 = methodResult("m-2", "LINKED");
        PaymentMethodResponse rp1 = methodResponse("m-1", "VERIFIED");
        PaymentMethodResponse rp2 = methodResponse("m-2", "LINKED");

        when(listPaymentMethodsInputPort.execute("user-123")).thenReturn(List.of(r1, r2));
        when(paymentMethodDtoMapper.toResponses(List.of(r1, r2))).thenReturn(List.of(rp1, rp2));

        mockMvc.perform(get("/api/v1/payments/methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].methodId").value("m-1"))
                .andExpect(jsonPath("$.data[1].methodId").value("m-2"));

        verify(listPaymentMethodsInputPort).execute("user-123");
    }

    @Test
    void getPreferenceReturnsPreference() throws Exception {
        PaymentPreferenceResult result = new PaymentPreferenceResult("CARD", "m-1");
        PaymentPreferenceResponse response = new PaymentPreferenceResponse("CARD", "m-1");

        when(getPaymentPreferenceInputPort.execute("user-123")).thenReturn(result);
        when(paymentPreferenceDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/methods/preference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentType").value("CARD"))
                .andExpect(jsonPath("$.data.paymentMethodId").value("m-1"));
    }

    @Test
    void updatePreferenceReturnsUpdated() throws Exception {
        PaymentPreferenceResult result = new PaymentPreferenceResult("CARD", "m-2");
        PaymentPreferenceResponse response = new PaymentPreferenceResponse("CARD", "m-2");

        when(updatePaymentPreferenceInputPort.execute("user-123", "CARD", "m-2")).thenReturn(result);
        when(paymentPreferenceDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(put("/api/v1/payments/methods/preference")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentType": "CARD",
                                  "paymentMethodId": "m-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentMethodId").value("m-2"));
    }

    @Test
    void createStripeSetupIntentReturnsClientSecret() throws Exception {
        StripeSetupIntentResult result = new StripeSetupIntentResult("si-1", "secret-1");
        StripeSetupIntentResponse response = new StripeSetupIntentResponse("si-1", "secret-1");

        when(createStripeSetupIntentInputPort.execute("user-123")).thenReturn(result);
        when(paymentMethodDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/methods/stripe/setup-intents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.setupIntentId").value("si-1"))
                .andExpect(jsonPath("$.data.clientSecret").value("secret-1"));
    }

    @Test
    void completeStripeSetupIntentLinksMethod() throws Exception {
        PaymentMethodResult result = methodResult("m-9", "VERIFIED");
        PaymentMethodResponse response = methodResponse("m-9", "VERIFIED");

        when(completeStripeSetupIntentInputPort.execute("user-123", "si-1")).thenReturn(result);
        when(paymentMethodDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/methods/stripe/setup-intents/complete")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "setupIntentId": "si-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.methodId").value("m-9"));
    }
}
