package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.exception.PromotionExceptionHandler;
import com.aionn.promotion.adapter.rest.mapper.media.PromotionMediaDtoMapperImpl;
import com.aionn.promotion.application.dto.media.result.UploadSignatureResult;
import com.aionn.promotion.application.port.in.media.GenerateBannerUploadSignatureInputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PromotionMediaControllerWebTest {

    @Mock
    private GenerateBannerUploadSignatureInputPort generateBannerUploadSignatureInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PromotionMediaController controller = new PromotionMediaController(
                generateBannerUploadSignatureInputPort, new PromotionMediaDtoMapperImpl());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PromotionExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JacksonMapperFactory.create()))
                .build();
    }

    @Test
    void returnsSignedUploadParameters() throws Exception {
        when(generateBannerUploadSignatureInputPort.execute()).thenReturn(
                new UploadSignatureResult("sig", "1750000000", "api-key", "demo-cloud",
                        "https://api.cloudinary.com/v1_1/demo-cloud/image/upload",
                        "aionn/promotion/banners"));

        mockMvc.perform(post("/api/v1/promotions/media/upload-signatures/banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signature").value("sig"))
                .andExpect(jsonPath("$.data.folder").value("aionn/promotion/banners"))
                .andExpect(jsonPath("$.message").value("Promotion banner upload signature generated"));
    }
}
