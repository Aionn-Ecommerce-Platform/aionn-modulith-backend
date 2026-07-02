package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.kyc.request.CreateKycRequest;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycDocumentResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycResponse;
import com.aionn.identity.adapter.rest.dto.kyc.response.KycVerificationSessionResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.kyc.KycDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.kyc.command.CreateKycCommand;
import com.aionn.identity.application.dto.kyc.query.GetKycQuery;
import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.application.dto.kyc.result.KycVerificationSessionResult;
import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;
import com.aionn.identity.application.port.in.kyc.AttachKycDocumentInputPort;
import com.aionn.identity.application.port.in.kyc.CreateKycInputPort;
import com.aionn.identity.application.port.in.kyc.GenerateKycVerificationSessionInputPort;
import com.aionn.identity.application.port.in.kyc.GetKycQueryPort;
import com.aionn.identity.application.port.in.kyc.ListKycDocumentsQueryPort;
import com.aionn.identity.application.port.in.kyc.ListMyKycQueryPort;
import com.aionn.identity.application.port.in.kyc.SubmitKycInputPort;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class KycControllerWebTest {

        @Mock
        private ListMyKycQueryPort listMyKycQueryPort;
        @Mock
        private GetKycQueryPort getKycQueryPort;
        @Mock
        private CreateKycInputPort createKycInputPort;
        @Mock
        private AttachKycDocumentInputPort attachKycDocumentInputPort;
        @Mock
        private ListKycDocumentsQueryPort listKycDocumentsQueryPort;
        @Mock
        private SubmitKycInputPort submitKycInputPort;
        @Mock
        private GenerateKycVerificationSessionInputPort generateKycVerificationSessionInputPort;
        @Mock
        private KycDtoMapper kycDtoMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                KycController controller = new KycController(listMyKycQueryPort, getKycQueryPort, createKycInputPort,
                                attachKycDocumentInputPort, listKycDocumentsQueryPort, submitKycInputPort,
                                generateKycVerificationSessionInputPort, kycDtoMapper);

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
        void listMyKycReturnsAllKycProfilesForUser() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                KycResult kyc1 = new KycResult("kyc-1", "user-123", "PASSPORT", "https://blob.url/doc1.pdf",
                                "PENDING", "SUMSUB", "applicant-123", "basic-kyc-level", "pending",
                                null, null, null, null, now, null);
                KycResult kyc2 = new KycResult("kyc-2", "user-123", "ID_CARD", "https://blob.url/doc2.pdf",
                                "APPROVED", "SUMSUB", "applicant-456", "basic-kyc-level", "completed",
                                "reviewer-789", "Approved", "admin-001", null, now.minusDays(5), now.minusDays(3));
                List<KycResult> results = List.of(kyc1, kyc2);

                KycResponse resp1 = new KycResponse("kyc-1", "user-123", "PASSPORT", "https://blob.url/doc1.pdf",
                                "PENDING", "SUMSUB", "applicant-123", "basic-kyc-level", "pending",
                                null, null, null, null, now, null);
                KycResponse resp2 = new KycResponse("kyc-2", "user-123", "ID_CARD", "https://blob.url/doc2.pdf",
                                "APPROVED", "SUMSUB", "applicant-456", "basic-kyc-level", "completed",
                                "reviewer-789", "Approved", "admin-001", null, now.minusDays(5), now.minusDays(3));

                when(listMyKycQueryPort.execute("alice@example.com")).thenReturn(results);
                when(kycDtoMapper.toResponses(results)).thenReturn(List.of(resp1, resp2));

                mockMvc.perform(get("/api/v1/kyc")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].kycId").value("kyc-1"))
                                .andExpect(jsonPath("$.data[1].status").value("APPROVED"));

                verify(listMyKycQueryPort).execute("alice@example.com");
        }

        @Test
        void getKycReturnsSpecificKycProfile() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                KycResult result = new KycResult("kyc-123", "user-456", "PASSPORT", "https://blob.url/doc.pdf",
                                "PENDING", "SUMSUB", "applicant-789", "basic-kyc-level", "pending",
                                null, null, null, null, now, null);
                KycResponse response = new KycResponse("kyc-123", "user-456", "PASSPORT", "https://blob.url/doc.pdf",
                                "PENDING", "SUMSUB", "applicant-789", "basic-kyc-level", "pending",
                                null, null, null, null, now, null);

                when(kycDtoMapper.toGetKycQuery("alice@example.com", "kyc-123"))
                                .thenReturn(new GetKycQuery("user-456", "kyc-123"));
                when(getKycQueryPort.execute(any())).thenReturn(result);
                when(kycDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(get("/api/v1/kyc/kyc-123")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.kycId").value("kyc-123"))
                                .andExpect(jsonPath("$.data.status").value("PENDING"));

                verify(getKycQueryPort).execute(any());
        }

        @Test
        void createKycCreatesNewKycProfile() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                KycResult result = new KycResult("kyc-new-789", "user-123", "ID_CARD", null,
                                "PENDING", null, null, null, null,
                                null, null, null, null, now, null);
                KycResponse response = new KycResponse("kyc-new-789", "user-123", "ID_CARD", null,
                                "PENDING", null, null, null, null,
                                null, null, null, null, now, null);

                when(kycDtoMapper.toCreateKycCommand(eq("alice@example.com"), any(CreateKycRequest.class)))
                                .thenReturn(new CreateKycCommand("user-123", "ID_CARD"));
                when(createKycInputPort.execute(any())).thenReturn(result);
                when(kycDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/kyc")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "docType": "ID_CARD"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.kycId").value("kyc-new-789"))
                                .andExpect(jsonPath("$.data.status").value("PENDING"));

                verify(createKycInputPort).execute(any());
        }

        @Test
        void attachDocumentAddsUploadedKycDocument() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                KycDocumentResult result = new KycDocumentResult(
                                "doc-1", "kyc-123", "ID_FRONT", "https://cdn.test/front.jpg",
                                "identity/kyc/user/front", "UPLOADED", now);
                KycDocumentResponse response = new KycDocumentResponse(
                                "doc-1", "kyc-123", "ID_FRONT", "https://cdn.test/front.jpg",
                                "identity/kyc/user/front", "UPLOADED", now);

                when(attachKycDocumentInputPort.execute(any())).thenReturn(result);
                when(kycDtoMapper.toDocumentResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/kyc/kyc-123/documents")
                                .with(user("alice@example.com").roles("USER"))
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "documentType": "ID_FRONT",
                                                  "url": "https://cdn.test/front.jpg",
                                                  "publicId": "identity/kyc/user/front"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.documentId").value("doc-1"))
                                .andExpect(jsonPath("$.data.type").value("ID_FRONT"));

                verify(attachKycDocumentInputPort).execute(any());
        }

        @Test
        void listDocumentsReturnsUploadedKycDocuments() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                KycDocumentResult result = new KycDocumentResult(
                                "doc-1", "kyc-123", "ID_BACK", "https://cdn.test/back.jpg",
                                null, "UPLOADED", now);
                KycDocumentResponse response = new KycDocumentResponse(
                                "doc-1", "kyc-123", "ID_BACK", "https://cdn.test/back.jpg",
                                null, "UPLOADED", now);

                when(listKycDocumentsQueryPort.execute("alice@example.com", "kyc-123"))
                                .thenReturn(List.of(result));
                when(kycDtoMapper.toDocumentResponses(List.of(result))).thenReturn(List.of(response));

                mockMvc.perform(get("/api/v1/kyc/kyc-123/documents")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].documentId").value("doc-1"))
                                .andExpect(jsonPath("$.data[0].type").value("ID_BACK"));

                verify(listKycDocumentsQueryPort).execute("alice@example.com", "kyc-123");
        }

        @Test
        void submitKycMovesProfileToSubmitted() throws Exception {
                LocalDateTime now = LocalDateTime.now();
                KycResult result = new KycResult("kyc-123", "user-123", "ID_CARD", "https://cdn.test/front.jpg",
                                "SUBMITTED", null, null, null, null,
                                null, null, null, null, now, null);
                KycResponse response = new KycResponse("kyc-123", "user-123", "ID_CARD", "https://cdn.test/front.jpg",
                                "SUBMITTED", null, null, null, null,
                                null, null, null, null, now, null);

                when(submitKycInputPort.execute("alice@example.com", "kyc-123")).thenReturn(result);
                when(kycDtoMapper.toResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/kyc/kyc-123/submit")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

                verify(submitKycInputPort).execute("alice@example.com", "kyc-123");
        }

        @Test
        void generateVerificationSessionReturnsSessionToken() throws Exception {
                KycVerificationSessionResult result = new KycVerificationSessionResult("kyc-123", "SUMSUB",
                                "applicant-xyz", "basic-kyc-level", "sdk-access-token-abc123", 600, true);
                KycVerificationSessionResponse response = new KycVerificationSessionResponse("kyc-123", "SUMSUB",
                                "applicant-xyz", "basic-kyc-level", "sdk-access-token-abc123", 600, true);

                when(generateKycVerificationSessionInputPort.execute("alice@example.com", "kyc-123"))
                                .thenReturn(result);
                when(kycDtoMapper.toVerificationSessionResponse(result)).thenReturn(response);

                mockMvc.perform(post("/api/v1/kyc/kyc-123/verification-session")
                                .with(user("alice@example.com").roles("USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.sdkAccessToken").value("sdk-access-token-abc123"))
                                .andExpect(jsonPath("$.data.provider").value("SUMSUB"));

                verify(generateKycVerificationSessionInputPort).execute("alice@example.com", "kyc-123");
        }

        @Test
        void unauthorizedRequestReturns401() throws Exception {
                mockMvc.perform(get("/api/v1/kyc"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(listMyKycQueryPort);
        }
}
