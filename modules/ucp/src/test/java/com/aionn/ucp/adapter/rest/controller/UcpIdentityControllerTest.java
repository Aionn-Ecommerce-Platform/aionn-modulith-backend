package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.advice.UcpControllerAdvice;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.identity.UcpIdentityApplicationService;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UcpIdentityControllerTest {

        @Mock
        private UcpIdentityApplicationService identityService;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .standaloneSetup(new UcpIdentityController(identityService))
                                .setControllerAdvice(new UcpControllerAdvice())
                                .build();
                objectMapper = new ObjectMapper();
        }

        @Test
        void postLinkReturnsCreatedWhenAuthorized() throws Exception {
                UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                                "google_assistant", "sub_123", "cust_456", List.of("orders.read"), Map.of());
                UcpIdentityLinkResponse response = new UcpIdentityLinkResponse(
                                "link_1", "google_assistant", "sub_123", "cust_456", "ACTIVE",
                                List.of("orders.read"), Instant.now(), null, Map.of());

                when(identityService.linkIdentity(any(), eq("cust_456"))).thenReturn(response);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "cust_456", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                mockMvc.perform(post("/ucp/v1/identity/links")
                                .principal(auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.linkId").value("link_1"))
                                .andExpect(jsonPath("$.platformSubject").value("sub_123"))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void getLinkReturnsOkWhenAuthorized() throws Exception {
                UcpIdentityLinkResponse response = new UcpIdentityLinkResponse(
                                "link_1", "google_assistant", "sub_123", "cust_456", "ACTIVE",
                                List.of("orders.read"), Instant.now(), null, Map.of());

                when(identityService.getLink("sub_123", "cust_456")).thenReturn(response);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "cust_456", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                mockMvc.perform(get("/ucp/v1/identity/links/sub_123")
                                .principal(auth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.linkId").value("link_1"))
                                .andExpect(jsonPath("$.customerId").value("cust_456"));
        }

        @Test
        void getLinkReturnsForbiddenWhenCallerDoesNotMatch() throws Exception {
                when(identityService.getLink("sub_123", "intruder"))
                                .thenThrow(new UcpProtocolException(403, "access_denied", "Caller is not authorized",
                                                "error"));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "intruder", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                mockMvc.perform(get("/ucp/v1/identity/links/sub_123")
                                .principal(auth))
                                .andExpect(status().isForbidden());
        }

        @Test
        void deleteLinkReturnsNoContentWhenAuthorized() throws Exception {
                doNothing().when(identityService).revokeLink("sub_123", "cust_456");

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "cust_456", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                mockMvc.perform(delete("/ucp/v1/identity/links/sub_123")
                                .principal(auth))
                                .andExpect(status().isNoContent());
        }
}
