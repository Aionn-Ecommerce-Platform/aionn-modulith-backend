package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.exception.PaymentExceptionHandler;
import com.aionn.payment.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.payment.application.port.in.stripeconnect.CreateStripeConnectOnboardingLinkInputPort;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StripeConnectControllerWebTest {

    @Mock
    private CreateStripeConnectOnboardingLinkInputPort createStripeConnectOnboardingLinkInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StripeConnectController controller = new StripeConnectController(createStripeConnectOnboardingLinkInputPort);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner-123", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateOnboardingLink() throws Exception {
        when(createStripeConnectOnboardingLinkInputPort.execute("owner-123")).thenReturn("https://stripe.com/onboard/123");

        mockMvc.perform(post("/api/v1/payments/stripe-connect/onboarding-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://stripe.com/onboard/123"));
    }
}
