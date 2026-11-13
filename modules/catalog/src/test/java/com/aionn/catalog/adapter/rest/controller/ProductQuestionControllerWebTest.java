package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.question.ProductQuestionDtoMapper;
import com.aionn.catalog.adapter.rest.support.MockOwnershipVerifier;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentMerchantIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.application.dto.question.result.ProductQuestionResult;
import com.aionn.catalog.application.service.ProductQuestionService;
import com.aionn.platform.infrastructure.config.JacksonMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductQuestionControllerWebTest {

        private static final String PRODUCT_ID = "product-1";
        private static final String QUESTION_ID = "question-1";
        private static final String MERCHANT_ID = "merchant-1";
        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private ProductQuestionService questionService;

        private MockMvc mockMvc;
        private final JsonMapper objectMapper = JacksonMapperFactory.create();

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(new ProductQuestionController(
                                questionService, Mappers.getMapper(ProductQuestionDtoMapper.class)))
                                .setControllerAdvice(new CatalogExceptionHandler())
                                .setCustomArgumentResolvers(
                                                new CurrentOwnerIdArgumentResolver(),
                                                new CurrentMerchantIdArgumentResolver(new MockOwnershipVerifier()))
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                                .build();
        }

        @Test
        void listByProductReturnsEnrichedQuestions() throws Exception {
                when(questionService.listByProduct(PRODUCT_ID, 0, 20))
                                .thenReturn(new ProductQuestionService.QuestionPage(List.of(result()), 1L));

                mockMvc.perform(get("/api/v1/catalog/products/" + PRODUCT_ID + "/questions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].questionId").value(QUESTION_ID))
                                .andExpect(jsonPath("$.data[0].userName").value("u1"))
                                .andExpect(jsonPath("$.data[0].userDisplayName").value("User One"))
                                .andExpect(jsonPath("$.data[0].userAvatarUrl").value("avatar.png"));
        }

        @Test
        void askReturnsTheCreatedQuestion() throws Exception {
                when(questionService.ask("user-1", PRODUCT_ID, "Why is it?"))
                                .thenReturn(result());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/questions")
                                .with(TestAuth.authUser("user-1"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"question":"Why is it?"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.questionId").value(QUESTION_ID));

                verify(questionService).ask("user-1", PRODUCT_ID, "Why is it?");
        }

        @Test
        void answerReturnsTheAnsweredQuestion() throws Exception {
                when(questionService.answer(MERCHANT_ID, QUESTION_ID, "Because"))
                                .thenReturn(result());

                mockMvc.perform(post("/api/v1/catalog/questions/" + QUESTION_ID + "/answer")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"answer":"Because"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.answer").value("Because"));

                verify(questionService).answer(MERCHANT_ID, QUESTION_ID, "Because");
        }

        @Test
        void deleteRemovesTheOwnersQuestion() throws Exception {
                mockMvc.perform(delete("/api/v1/catalog/questions/" + QUESTION_ID)
                                .with(TestAuth.authUser("user-1")))
                                .andExpect(status().isOk());

                verify(questionService).delete("user-1", QUESTION_ID);
        }

        @Test
        void listByMerchantReturnsTheMerchantsQuestions() throws Exception {
                when(questionService.listByMerchant(eq(MERCHANT_ID), eq(true), eq(0), eq(20)))
                                .thenReturn(new ProductQuestionService.QuestionPage(List.of(result()), 1L));

                mockMvc.perform(get("/api/v1/catalog/merchant/questions")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .param("answered", "true"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].questionId").value(QUESTION_ID));
        }

        private static ProductQuestionResult result() {
                return new ProductQuestionResult(QUESTION_ID, PRODUCT_ID, "user-1", "Why?",
                                "Because", MERCHANT_ID, NOW, NOW, "u1", "User One", "avatar.png");
        }
}
