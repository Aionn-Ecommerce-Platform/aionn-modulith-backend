package com.aionn.ucp.application.identity;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.port.out.UcpIdentityLinkPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UcpIdentityApplicationServiceTest {

        @Mock
        private UcpIdentityLinkPort identityLinkPort;

        private UcpIdentityApplicationService service;

        @BeforeEach
        void setUp() {
                service = new UcpIdentityApplicationService(identityLinkPort);
        }

        @Test
        void linkIdentitySucceedsWhenCallerMatchesCustomerId() {
                UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                                "google_assistant", "sub_123", "cust_456", List.of("orders.read"), Map.of());
                UcpIdentityLinkResponse expected = new UcpIdentityLinkResponse(
                                "link_1", "google_assistant", "sub_123", "cust_456", "ACTIVE",
                                List.of("orders.read"), Instant.now(), null, Map.of());

                when(identityLinkPort.findByPlatformAndSubject("google_assistant", "sub_123"))
                                .thenReturn(Optional.empty());
                when(identityLinkPort.saveLink(any())).thenReturn(expected);

                UcpIdentityLinkResponse result = service.linkIdentity(request, "cust_456");

                assertThat(result).isNotNull();
                assertThat(result.platformSubject()).isEqualTo("sub_123");
                assertThat(result.customerId()).isEqualTo("cust_456");
                assertThat(result.status()).isEqualTo("ACTIVE");
        }

        @Test
        void linkIdentityThrowsForbiddenWhenCallerDoesNotMatchCustomerId() {
                UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                                "google_assistant", "sub_123", "cust_456", List.of(), Map.of());

                assertThatThrownBy(() -> service.linkIdentity(request, "attacker_user"))
                                .isInstanceOf(UcpProtocolException.class)
                                .hasMessageContaining("Caller is not authorized");
        }

        @Test
        void linkIdentityThrowsConflictWhenPlatformSubjectBelongsToAnotherCustomer() {
                UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                                "google_assistant", "sub_123", "cust_456", List.of(), Map.of());
                UcpIdentityLinkResponse existingOtherCustomer = new UcpIdentityLinkResponse(
                                "link_old", "google_assistant", "sub_123", "different_cust", "ACTIVE",
                                List.of(), Instant.now(), null, Map.of());

                when(identityLinkPort.findByPlatformAndSubject("google_assistant", "sub_123"))
                                .thenReturn(Optional.of(existingOtherCustomer));

                assertThatThrownBy(() -> service.linkIdentity(request, "cust_456"))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(409);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("identity_conflict");
                                });
        }

        @Test
        void getLinkSucceedsWhenFoundAndCallerMatches() {
                UcpIdentityLinkResponse expected = new UcpIdentityLinkResponse(
                                "link_1", "google_assistant", "sub_123", "cust_456", "ACTIVE",
                                List.of(), Instant.now(), null, Map.of());

                when(identityLinkPort.findByPlatformAndSubject("google_assistant", "sub_123"))
                                .thenReturn(Optional.of(expected));

                UcpIdentityLinkResponse result = service.getLink("google_assistant", "sub_123", "cust_456");

                assertThat(result).isNotNull();
                assertThat(result.platformSubject()).isEqualTo("sub_123");
        }

        @Test
        void getLinkThrowsNotFoundWhenSubjectDoesNotExist() {
                when(identityLinkPort.findByPlatformAndSubject("google_assistant", "unknown_sub"))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.getLink("google_assistant", "unknown_sub", "cust_456"))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(404);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("link_not_found");
                                });
        }

        @Test
        void getLinkThrowsNotFoundWhenCallerDoesNotMatchCustomer() {
                UcpIdentityLinkResponse expected = new UcpIdentityLinkResponse(
                                "link_1", "google_assistant", "sub_123", "cust_456", "ACTIVE",
                                List.of(), Instant.now(), null, Map.of());

                when(identityLinkPort.findByPlatformAndSubject("google_assistant", "sub_123"))
                                .thenReturn(Optional.of(expected));

                // Maps unauthorized access to 404 to avoid leaking subject existence
                assertThatThrownBy(() -> service.getLink("google_assistant", "sub_123", "attacker_user"))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(404);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("link_not_found");
                                });
        }

        @Test
        void revokeLinkSucceedsWhenFoundAndCallerMatches() {
                UcpIdentityLinkResponse expected = new UcpIdentityLinkResponse(
                                "link_1", "google_assistant", "sub_123", "cust_456", "ACTIVE",
                                List.of(), Instant.now(), null, Map.of());

                when(identityLinkPort.findByPlatformAndSubject("google_assistant", "sub_123"))
                                .thenReturn(Optional.of(expected));

                service.revokeLink("google_assistant", "sub_123", "cust_456");

                verify(identityLinkPort).revokeLink("google_assistant", "sub_123");
        }

        @Test
        void revokeLinkThrowsNotFoundWhenCallerDoesNotMatchCustomer() {
                UcpIdentityLinkResponse expected = new UcpIdentityLinkResponse(
                                "link_1", "google_assistant", "sub_123", "cust_456", "ACTIVE",
                                List.of(), Instant.now(), null, Map.of());

                when(identityLinkPort.findByPlatformAndSubject("google_assistant", "sub_123"))
                                .thenReturn(Optional.of(expected));

                assertThatThrownBy(() -> service.revokeLink("google_assistant", "sub_123", "attacker_user"))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(404);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("link_not_found");
                                });
        }
}
