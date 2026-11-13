package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.merchant.MerchantDtoMapper;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.OptionalOwnerIdArgumentResolver;
import com.aionn.catalog.application.dto.merchant.result.MerchantStorefrontStatsResult;
import com.aionn.catalog.application.service.MerchantFollowService;
import com.aionn.catalog.application.service.MerchantStorefrontStatsService;
import com.aionn.platform.infrastructure.config.JacksonMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantFollowControllerWebTest {

    private static final String MERCHANT_ID = "merchant-1";

    @Mock
    private MerchantFollowService followService;
    @Mock
    private MerchantStorefrontStatsService statsService;

    private MockMvc mockMvc;
    private final JsonMapper objectMapper = JacksonMapperFactory.create();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MerchantFollowController(followService, statsService,
                        Mappers.getMapper(MerchantDtoMapper.class)))
                .setControllerAdvice(new CatalogExceptionHandler())
                .setCustomArgumentResolvers(new CurrentOwnerIdArgumentResolver(),
                        new OptionalOwnerIdArgumentResolver())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void followRecordsTheFollower() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/merchants/" + MERCHANT_ID + "/follow")
                .with(TestAuth.authUser("user-1")))
                .andExpect(status().isOk());

        verify(followService).follow(MERCHANT_ID, "user-1");
    }

    @Test
    void unfollowRemovesTheFollower() throws Exception {
        mockMvc.perform(delete("/api/v1/catalog/merchants/" + MERCHANT_ID + "/follow")
                .with(TestAuth.authUser("user-1")))
                .andExpect(status().isOk());

        verify(followService).unfollow(MERCHANT_ID, "user-1");
    }

    @Test
    void followRequiresAnAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/merchants/" + MERCHANT_ID + "/follow"))
                .andExpect(status().isForbidden());
    }

    @Test
    void followerCountIsPublic() throws Exception {
        when(followService.followerCount(MERCHANT_ID)).thenReturn(12L);

        mockMvc.perform(get("/api/v1/catalog/merchants/" + MERCHANT_ID + "/followers/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(12));
    }

    @Test
    void statsPassTheAuthenticatedUser() throws Exception {
        when(statsService.stats(MERCHANT_ID, "user-1")).thenReturn(stats(true));

        mockMvc.perform(get("/api/v1/catalog/merchants/" + MERCHANT_ID + "/storefront-stats")
                .with(TestAuth.authUser("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.following").value(true))
                .andExpect(jsonPath("$.data.responseTimeBucket").value("MINUTES"));
    }

    @Test
    void statsTreatAnonymousVisitorsAsSignedOut() throws Exception {
        when(statsService.stats(MERCHANT_ID, null)).thenReturn(stats(false));

        mockMvc.perform(get("/api/v1/catalog/merchants/" + MERCHANT_ID + "/storefront-stats")
                .with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.following").value(false));
    }

    private static RequestPostProcessor anonymous() {
        return request -> {
            var authentication = new AnonymousAuthenticationToken("key", "anonymous",
                    List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
            return request;
        };
    }

    private static MerchantStorefrontStatsResult stats(boolean following) {
        return new MerchantStorefrontStatsResult(3, 4.5, 7, 12, Instant.parse("2026-01-01T00:00:00Z"),
                10, 8, 60L, 80, "MINUTES", following);
    }
}
