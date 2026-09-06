package com.aionn.recommendation.adapter.rest.controller;

import com.aionn.recommendation.adapter.rest.exception.RecommendationExceptionHandler;
import com.aionn.recommendation.adapter.rest.mapper.RecommendationDtoMapper;
import org.mapstruct.factory.Mappers;
import com.aionn.recommendation.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.recommendation.adapter.rest.support.TestAuth;
import com.aionn.recommendation.application.dto.query.GetAlsoBoughtQuery;
import com.aionn.recommendation.application.dto.query.GetCartSuggestionsQuery;
import com.aionn.recommendation.application.dto.query.GetHomeFeedQuery;
import com.aionn.recommendation.application.dto.query.GetSimilarProductsQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.in.GetAlsoBoughtInputPort;
import com.aionn.recommendation.application.port.in.GetCartSuggestionsInputPort;
import com.aionn.recommendation.application.port.in.GetHomeFeedInputPort;
import com.aionn.recommendation.application.port.in.GetSimilarProductsInputPort;
import com.aionn.recommendation.domain.valueobject.RecommendationReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerWebTest {

        private static final String PRODUCT_ID = "01HZPRD0000000000000000001";

        @Mock
        private GetHomeFeedInputPort getHomeFeedInputPort;
        @Mock
        private GetSimilarProductsInputPort getSimilarProductsInputPort;
        @Mock
        private GetAlsoBoughtInputPort getAlsoBoughtInputPort;
        @Mock
        private GetCartSuggestionsInputPort getCartSuggestionsInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                RecommendationController controller = new RecommendationController(
                                getHomeFeedInputPort,
                                getSimilarProductsInputPort,
                                getAlsoBoughtInputPort,
                                getCartSuggestionsInputPort,
                                Mappers.getMapper(RecommendationDtoMapper.class));
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new RecommendationExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .build();
        }

        @Test
        void homeFeedReturnsRankedItemsWithTheirReason() throws Exception {
                when(getHomeFeedInputPort.execute(any(GetHomeFeedQuery.class)))
                                .thenReturn(List.of(item("p-1", RecommendationReason.SIMILAR_TO_VIEWED)));

                mockMvc.perform(get("/api/v1/recommendations/home")
                                .with(TestAuth.authUser("user-1")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value("p-1"))
                                .andExpect(jsonPath("$.data[0].reason").value("SIMILAR_TO_VIEWED"));
        }

        @Test
        void theAuthenticatedPrincipalIsPassedThroughAsTheUserId() throws Exception {
                when(getHomeFeedInputPort.execute(any(GetHomeFeedQuery.class))).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/recommendations/home")
                                .with(TestAuth.authUser("user-42")))
                                .andExpect(status().isOk());

                ArgumentCaptor<GetHomeFeedQuery> query = ArgumentCaptor.forClass(GetHomeFeedQuery.class);
                verify(getHomeFeedInputPort).execute(query.capture());
                assertThat(query.getValue().userId()).isEqualTo("user-42");
        }

        @Test
        void anAnonymousHomeFeedRequestIsAllowedAndCarriesNoUserId() throws Exception {
                // The endpoint is intentionally open: a visitor who has not logged in still
                // gets trending.
                when(getHomeFeedInputPort.execute(any(GetHomeFeedQuery.class))).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/recommendations/home")).andExpect(status().isOk());

                ArgumentCaptor<GetHomeFeedQuery> query = ArgumentCaptor.forClass(GetHomeFeedQuery.class);
                verify(getHomeFeedInputPort).execute(query.capture());
                assertThat(query.getValue().userId()).isNull();
        }

        @Test
        void theDefaultLimitIsApplied() throws Exception {
                when(getHomeFeedInputPort.execute(any(GetHomeFeedQuery.class))).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/recommendations/home")).andExpect(status().isOk());

                ArgumentCaptor<GetHomeFeedQuery> query = ArgumentCaptor.forClass(GetHomeFeedQuery.class);
                verify(getHomeFeedInputPort).execute(query.capture());
                assertThat(query.getValue().limit()).isEqualTo(10);
        }

        @Test
        void similarProductsReturnsOk() throws Exception {
                when(getSimilarProductsInputPort.execute(any(GetSimilarProductsQuery.class)))
                                .thenReturn(List.of(item("p-2", RecommendationReason.SIMILAR_TO_VIEWED)));

                mockMvc.perform(get("/api/v1/recommendations/products/" + PRODUCT_ID + "/similar"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value("p-2"));
        }

        @Test
        void alsoBoughtReturnsOk() throws Exception {
                when(getAlsoBoughtInputPort.execute(any(GetAlsoBoughtQuery.class)))
                                .thenReturn(List.of(item("p-3", RecommendationReason.FREQUENTLY_BOUGHT_TOGETHER)));

                mockMvc.perform(get("/api/v1/recommendations/products/" + PRODUCT_ID + "/also-bought"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].reason").value("FREQUENTLY_BOUGHT_TOGETHER"));
        }

        @Test
        void cartSuggestionsPassTheBasketThrough() throws Exception {
                when(getCartSuggestionsInputPort.execute(any(GetCartSuggestionsQuery.class)))
                                .thenReturn(List.of(item("p-4", RecommendationReason.FREQUENTLY_BOUGHT_TOGETHER)));

                mockMvc.perform(get("/api/v1/recommendations/cart/suggestions")
                                .param("skuIds", "sku-1", "sku-2")
                                .with(TestAuth.authUser("user-1")))
                                .andExpect(status().isOk());

                ArgumentCaptor<GetCartSuggestionsQuery> query = ArgumentCaptor.forClass(GetCartSuggestionsQuery.class);
                verify(getCartSuggestionsInputPort).execute(query.capture());
                assertThat(query.getValue().cartSkuIds()).containsExactly("sku-1", "sku-2");
                assertThat(query.getValue().userId()).isEqualTo("user-1");
        }

        @Test
        void internalSkuIdsAreNotExposedToClients() throws Exception {
                // skuIds ride along on the application result purely so availability filtering
                // can run after the
                // cache; clients recommend products, not SKUs.
                when(getHomeFeedInputPort.execute(any(GetHomeFeedQuery.class)))
                                .thenReturn(List.of(item("p-1", RecommendationReason.TRENDING)));

                mockMvc.perform(get("/api/v1/recommendations/home"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].skuIds").doesNotExist());
        }

        private static RecommendationItemResult item(String productId, RecommendationReason reason) {
                return new RecommendationItemResult(
                                productId,
                                "Product " + productId,
                                "https://cdn.example.com/" + productId + ".jpg",
                                BigDecimal.valueOf(34_990_000),
                                "VND",
                                BigDecimal.valueOf(0.91),
                                reason,
                                List.of("sku-" + productId));
        }
}
