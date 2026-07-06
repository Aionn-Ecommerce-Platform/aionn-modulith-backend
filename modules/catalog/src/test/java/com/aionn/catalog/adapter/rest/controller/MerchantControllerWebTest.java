package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.merchant.AdminReasonRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.RegisterMerchantRequest;
import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.SuspendMerchantCommand;
import com.aionn.catalog.application.dto.merchant.query.GetMerchantByOwnerQuery;
import com.aionn.catalog.application.dto.merchant.query.GetMerchantQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.ActivateMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.CloseMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.GetMerchantByOwnerInputPort;
import com.aionn.catalog.application.port.in.merchant.GetMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.ListMerchantsInputPort;
import com.aionn.catalog.application.port.in.merchant.RegisterMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.SuspendMerchantInputPort;
import com.aionn.catalog.application.port.in.merchant.UpdateMerchantProfileInputPort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantControllerWebTest {

        @Mock
        private RegisterMerchantInputPort registerMerchantInputPort;
        @Mock
        private UpdateMerchantProfileInputPort updateMerchantProfileInputPort;
        @Mock
        private SuspendMerchantInputPort suspendMerchantInputPort;
        @Mock
        private ActivateMerchantInputPort activateMerchantInputPort;
        @Mock
        private CloseMerchantInputPort closeMerchantInputPort;
        @Mock
        private GetMerchantByOwnerInputPort getMerchantByOwnerInputPort;
        @Mock
        private GetMerchantInputPort getMerchantInputPort;
        @Mock
        private ListMerchantsInputPort listMerchantsInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.merchant.UpdateGlobalCommissionRateInputPort updateGlobalCommissionRateInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.merchant.UpdateMerchantCommissionRateInputPort updateMerchantCommissionRateInputPort;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

        @BeforeEach
        void setUp() {
                MerchantController controller = new MerchantController(
                                registerMerchantInputPort, updateMerchantProfileInputPort, suspendMerchantInputPort,
                                activateMerchantInputPort, closeMerchantInputPort, getMerchantByOwnerInputPort,
                                getMerchantInputPort, listMerchantsInputPort,
                                new com.aionn.catalog.adapter.rest.mapper.merchant.MerchantDtoMapperImpl(),
                                updateGlobalCommissionRateInputPort, updateMerchantCommissionRateInputPort);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new CatalogExceptionHandler())
                                .setCustomArgumentResolvers(
                                                new CurrentOwnerIdArgumentResolver(),
                                                new CurrentAdminIdArgumentResolver())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                                .build();
        }

        private MerchantResult sample() {
                return new MerchantResult(
                                "m-1", "owner-1", "Acme", null, "desc", "01", "Ha Noi",
                                "ACTIVE", Instant.now(), Instant.now());
        }

        @Test
        void registerReturnsCreatedAndPassesPrincipalToService() throws Exception {
                when(registerMerchantInputPort.execute(any(RegisterMerchantCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/merchants")
                                .with(TestAuth.authUser("owner-1", "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RegisterMerchantRequest("Acme Store"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.merchantId").value("m-1"))
                                .andExpect(jsonPath("$.data.ownerId").value("owner-1"));

                verify(registerMerchantInputPort).execute(any(RegisterMerchantCommand.class));
        }

        @Test
        void getReturnsMerchant() throws Exception {
                when(getMerchantInputPort.execute(any(GetMerchantQuery.class))).thenReturn(sample());

                mockMvc.perform(get("/api/v1/catalog/merchants/m-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.merchantId").value("m-1"))
                                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        void getMineResolvesByOwnerId() throws Exception {
                when(getMerchantByOwnerInputPort.execute(any(GetMerchantByOwnerQuery.class))).thenReturn(sample());

                mockMvc.perform(get("/api/v1/catalog/merchants/me")
                                .with(TestAuth.authUser("owner-1", "USER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.ownerId").value("owner-1"));

                verify(getMerchantByOwnerInputPort).execute(any(GetMerchantByOwnerQuery.class));
        }

        @Test
        void suspendDelegatesAdminReasonToService() throws Exception {
                when(suspendMerchantInputPort.execute(any(SuspendMerchantCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/merchants/m-1/suspend")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AdminReasonRequest("policy violation"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.merchantId").value("m-1"));

                verify(suspendMerchantInputPort).execute(any(SuspendMerchantCommand.class));
        }

        @Test
        void getReturnsNotFoundWhenServiceThrows() throws Exception {
                when(getMerchantInputPort.execute(any(GetMerchantQuery.class)))
                                .thenThrow(new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));

                mockMvc.perform(get("/api/v1/catalog/merchants/missing"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void activateDelegatesAdminReason() throws Exception {
                when(activateMerchantInputPort.execute(
                                any(com.aionn.catalog.application.dto.merchant.command.ActivateMerchantCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/merchants/m-1/activate")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AdminReasonRequest("reinstate"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.merchantId").value("m-1"));

                verify(activateMerchantInputPort)
                                .execute(any(com.aionn.catalog.application.dto.merchant.command.ActivateMerchantCommand.class));
        }

        @Test
        void closeUsesOwnerPrincipal() throws Exception {
                when(closeMerchantInputPort.execute(
                                any(com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/merchants/m-1/close")
                                .with(TestAuth.authUser("owner-1", "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AdminReasonRequest("shutdown"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.merchantId").value("m-1"));

                verify(closeMerchantInputPort)
                                .execute(any(com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand.class));
        }

        @Test
        void listReturnsPagedMerchants() throws Exception {
                com.aionn.catalog.application.dto.common.PageResult<MerchantResult> page = new com.aionn.catalog.application.dto.common.PageResult<>(
                                java.util.List.of(sample()), 0, 20, 1);
                when(listMerchantsInputPort.execute(any(
                                com.aionn.catalog.application.dto.merchant.query.ListMerchantsQuery.class)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/v1/catalog/merchants"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].merchantId").value("m-1"));

                verify(listMerchantsInputPort).execute(any(
                                com.aionn.catalog.application.dto.merchant.query.ListMerchantsQuery.class));
        }

        @Test
        void updateGlobalCommissionRateReturnsOk() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/v1/catalog/merchants/settings/commission-rate")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new com.aionn.catalog.adapter.rest.dto.merchant.UpdateCommissionRateRequest(
                                                                new java.math.BigDecimal("0.0600")))))
                                .andExpect(status().isOk());

                verify(updateGlobalCommissionRateInputPort).execute(any(
                                com.aionn.catalog.application.dto.merchant.command.UpdateGlobalCommissionRateCommand.class));
        }

        @Test
        void updateMerchantCommissionRateReturnsUpdatedMerchant() throws Exception {
                when(updateMerchantCommissionRateInputPort.execute(any(
                                com.aionn.catalog.application.dto.merchant.command.UpdateMerchantCommissionRateCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .put("/api/v1/catalog/merchants/m-1/commission-rate")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new com.aionn.catalog.adapter.rest.dto.merchant.UpdateCommissionRateRequest(
                                                                new java.math.BigDecimal("0.0800")))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.merchantId").value("m-1"));
        }
}
