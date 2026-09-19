package com.aionn.ucp.infrastructure.webhook;

import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestClientUcpWebhookDispatcherTest {

        @Mock
        private RestClient restClient;
        @Mock
        private RestClient.RequestBodyUriSpec requestBodyUriSpec;
        @Mock
        private RestClient.RequestBodySpec requestBodySpec;
        @Mock
        private RestClient.ResponseSpec responseSpec;
        @Mock
        private com.aionn.ucp.application.port.out.UcpMetricsPort metricsPort;

        private RestClientUcpWebhookDispatcher dispatcher;

        @BeforeEach
        void setUp() {
                dispatcher = new RestClientUcpWebhookDispatcher(restClient, metricsPort);
        }

        @Test
        void dispatchReturnsTrueOnSuccessfulPost() {
                UcpWebhookEvent event = new UcpWebhookEvent(
                                "evt_1", "order.shipped", "ord_1", Instant.now(), Map.of());

                when(restClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri("https://example.com/webhook")).thenReturn(requestBodySpec);
                when(requestBodySpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                                .thenReturn(requestBodySpec);
                when(requestBodySpec.body(event)).thenReturn(requestBodySpec);
                when(requestBodySpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

                boolean result = dispatcher.dispatch("https://example.com/webhook", event);
                assertThat(result).isTrue();
                org.mockito.Mockito.verify(metricsPort).recordWebhookDispatch("order.shipped", true);
        }

        @Test
        void dispatchReturnsFalseWhenUrlOrEventIsNull() {
                assertThat(dispatcher.dispatch(null, null)).isFalse();
                assertThat(dispatcher.dispatch("   ", new UcpWebhookEvent("e1", "t1", "o1", Instant.now(), Map.of())))
                                .isFalse();
                assertThat(dispatcher.dispatch("https://example.com", null)).isFalse();
        }

        @Test
        void dispatchReturnsFalseOnErrorResponse() {
                UcpWebhookEvent event = new UcpWebhookEvent(
                                "evt_1", "order.completed", "ord_1", Instant.now(), Map.of());

                when(restClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri("https://example.com/webhook")).thenReturn(requestBodySpec);
                when(requestBodySpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                                .thenReturn(requestBodySpec);
                when(requestBodySpec.body(event)).thenReturn(requestBodySpec);
                when(requestBodySpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("Connection refused"));

                boolean result = dispatcher.dispatch("https://example.com/webhook", event);
                assertThat(result).isFalse();
                org.mockito.Mockito.verify(metricsPort).recordWebhookDispatch("order.completed", false);
        }

        @Test
        void dispatchRejectsSsrfTargets() {
                UcpWebhookEvent event = new UcpWebhookEvent(
                                "evt_1", "order.completed", "ord_1", Instant.now(), Map.of());

                assertThat(dispatcher.dispatch("http://localhost:8080/webhook", event)).isFalse();
                assertThat(dispatcher.dispatch("http://127.0.0.1:8080/webhook", event)).isFalse();
                assertThat(dispatcher.dispatch("http://10.0.0.1/webhook", event)).isFalse();
                assertThat(dispatcher.dispatch("http://192.168.1.1/webhook", event)).isFalse();
                assertThat(dispatcher.dispatch("http://172.16.0.1/webhook", event)).isFalse();
                assertThat(dispatcher.dispatch("http://169.254.169.254/latest/meta-data", event)).isFalse();
                assertThat(dispatcher.dispatch("ftp://example.com/webhook", event)).isFalse();
                assertThat(dispatcher.dispatch("://invalid-uri", event)).isFalse();
                assertThat(dispatcher.dispatch("http://this-host-does-not-exist.invalid/webhook", event)).isFalse();
        }

        @Test
        void alternateConstructorsInitializeProperly() {
                RestClientUcpWebhookDispatcher singleArg = new RestClientUcpWebhookDispatcher(restClient);
                assertThat(singleArg).isNotNull();

                RestClientUcpWebhookDispatcher builderArg = new RestClientUcpWebhookDispatcher(
                                java.util.Optional.of(RestClient.builder()),
                                java.util.Optional.of(metricsPort));
                assertThat(builderArg).isNotNull();

                RestClientUcpWebhookDispatcher emptyOptionalArg = new RestClientUcpWebhookDispatcher(
                                java.util.Optional.empty(),
                                java.util.Optional.empty());
                assertThat(emptyOptionalArg).isNotNull();

                RestClientUcpWebhookDispatcher nullOptionalArg = new RestClientUcpWebhookDispatcher(
                                (java.util.Optional<RestClient.Builder>) null,
                                null);
                assertThat(nullOptionalArg).isNotNull();
        }

        @Test
        void dispatchReturnsFalseOnNon2xxResponse() {
                UcpWebhookEvent event = new UcpWebhookEvent(
                                "evt_1", "order.completed", "ord_1", Instant.now(), Map.of());

                when(restClient.post()).thenReturn(requestBodyUriSpec);
                when(requestBodyUriSpec.uri("https://example.com/webhook")).thenReturn(requestBodySpec);
                when(requestBodySpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                                .thenReturn(requestBodySpec);
                when(requestBodySpec.body(event)).thenReturn(requestBodySpec);
                when(requestBodySpec.retrieve()).thenReturn(responseSpec);
                when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.status(500).build());

                boolean result = dispatcher.dispatch("https://example.com/webhook", event);
                assertThat(result).isFalse();
                org.mockito.Mockito.verify(metricsPort).recordWebhookDispatch("order.completed", false);
        }
}
