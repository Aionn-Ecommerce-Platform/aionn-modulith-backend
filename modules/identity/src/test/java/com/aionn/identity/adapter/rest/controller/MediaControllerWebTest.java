package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.media.MediaDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import com.aionn.identity.application.port.in.media.GenerateAvatarUploadSignatureInputPort;
import com.aionn.identity.application.port.in.media.GenerateKycDocumentUploadSignatureInputPort;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web tests for MediaController. Verifies the avatar upload signature endpoint
 * returns Cloudinary signing parameters for the authenticated user and rejects
 * unauthenticated requests.
 */
@ExtendWith(MockitoExtension.class)
class MediaControllerWebTest {

    @Mock
    private GenerateAvatarUploadSignatureInputPort generateAvatarUploadSignatureInputPort;
    @Mock
    private GenerateKycDocumentUploadSignatureInputPort generateKycDocumentUploadSignatureInputPort;
    @Mock
    private MediaDtoMapper mediaDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MediaController controller = new MediaController(
                generateAvatarUploadSignatureInputPort,
                generateKycDocumentUploadSignatureInputPort,
                mediaDtoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(
                        new ClientIpArgumentResolver(new ClientIpResolver()),
                        new MockAuthenticationArgumentResolver())
                .build();
    }

    @Test
    void generateAvatarSignatureReturnsCloudinarySigningParams() throws Exception {
        UploadSignatureResult result = new UploadSignatureResult(
                "abcdef1234567890signature",
                "1717000000",
                "cloudinary-api-key",
                "aionn-cloud",
                "https://api.cloudinary.com/v1_1/aionn-cloud/image/upload",
                "identity/avatars");
        UploadSignatureResponse response = new UploadSignatureResponse(
                "abcdef1234567890signature",
                "1717000000",
                "cloudinary-api-key",
                "aionn-cloud",
                "https://api.cloudinary.com/v1_1/aionn-cloud/image/upload",
                "identity/avatars");

        when(generateAvatarUploadSignatureInputPort.execute("alice@example.com")).thenReturn(result);
        when(mediaDtoMapper.toUploadSignatureResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/media/upload-signatures/avatar")
                .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signature").value("abcdef1234567890signature"))
                .andExpect(jsonPath("$.data.timestamp").value("1717000000"))
                .andExpect(jsonPath("$.data.apiKey").value("cloudinary-api-key"))
                .andExpect(jsonPath("$.data.cloudName").value("aionn-cloud"))
                .andExpect(jsonPath("$.data.folder").value("identity/avatars"));

        verify(generateAvatarUploadSignatureInputPort).execute("alice@example.com");
    }

    @Test
    void generateKycDocumentSignatureReturnsCloudinarySigningParams() throws Exception {
        UploadSignatureResult result = new UploadSignatureResult(
                "kyc-signature",
                "1717000001",
                "cloudinary-api-key",
                "aionn-cloud",
                "https://api.cloudinary.com/v1_1/aionn-cloud/auto/upload",
                "identity/kyc/alice@example.com");
        UploadSignatureResponse response = new UploadSignatureResponse(
                "kyc-signature",
                "1717000001",
                "cloudinary-api-key",
                "aionn-cloud",
                "https://api.cloudinary.com/v1_1/aionn-cloud/auto/upload",
                "identity/kyc/alice@example.com");

        when(generateKycDocumentUploadSignatureInputPort.execute("alice@example.com")).thenReturn(result);
        when(mediaDtoMapper.toUploadSignatureResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/media/upload-signatures/kyc")
                .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signature").value("kyc-signature"))
                .andExpect(jsonPath("$.data.folder").value("identity/kyc/alice@example.com"));

        verify(generateKycDocumentUploadSignatureInputPort).execute("alice@example.com");
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/media/upload-signatures/avatar"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(generateAvatarUploadSignatureInputPort);
    }
}
