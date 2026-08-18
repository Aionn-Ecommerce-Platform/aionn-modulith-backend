package com.aionn.promotion.adapter.rest.controller;

import com.aionn.promotion.adapter.rest.exception.PromotionExceptionHandler;
import com.aionn.promotion.adapter.rest.mapper.banner.PromotionBannerDtoMapperImpl;
import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.port.in.banner.CreateBannerInputPort;
import com.aionn.promotion.application.port.in.banner.DeleteBannerInputPort;
import com.aionn.promotion.application.port.in.banner.GetBannerInputPort;
import com.aionn.promotion.application.port.in.banner.ListActiveBannersInputPort;
import com.aionn.promotion.application.port.in.banner.ListAllBannersInputPort;
import com.aionn.promotion.application.port.in.banner.UpdateBannerInputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PromotionBannerControllerWebTest {

    @Mock
    private ListActiveBannersInputPort listActiveBannersInputPort;
    @Mock
    private ListAllBannersInputPort listAllBannersInputPort;
    @Mock
    private GetBannerInputPort getBannerInputPort;
    @Mock
    private CreateBannerInputPort createBannerInputPort;
    @Mock
    private UpdateBannerInputPort updateBannerInputPort;
    @Mock
    private DeleteBannerInputPort deleteBannerInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PromotionBannerController controller = new PromotionBannerController(
                listActiveBannersInputPort, listAllBannersInputPort, getBannerInputPort,
                createBannerInputPort, updateBannerInputPort, deleteBannerInputPort,
                new PromotionBannerDtoMapperImpl());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PromotionExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JacksonMapperFactory.create()))
                .build();
    }

    private static PromotionBannerResult sample(String id) {
        return new PromotionBannerResult(id, "Title " + id, "https://cdn/" + id + ".png",
                "aionn/promotion/banners/" + id, "https://shop/" + id, 1);
    }

    @Test
    void publicListReturnsActiveBanners() throws Exception {
        when(listActiveBannersInputPort.execute(any()))
                .thenReturn(new com.aionn.promotion.application.dto.common.PageResult<>(
                        List.of(sample("BAN_1")), 0, 20, 1));

        mockMvc.perform(get("/api/v1/promotions/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bannerId").value("BAN_1"))
                .andExpect(jsonPath("$.paging.totalElements").value(1))
                .andExpect(jsonPath("$.message").value("Promotion banners fetched"));
    }

    @Test
    void adminListReturnsAllBanners() throws Exception {
        when(listAllBannersInputPort.execute(any()))
                .thenReturn(new com.aionn.promotion.application.dto.common.PageResult<>(
                        List.of(sample("BAN_1"), sample("BAN_2")), 0, 20, 2));

        mockMvc.perform(get("/api/v1/promotions/banners/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].bannerId").value("BAN_2"));
    }

    @Test
    void adminGetReturnsBanner() throws Exception {
        when(getBannerInputPort.execute("BAN_1")).thenReturn(sample("BAN_1"));

        mockMvc.perform(get("/api/v1/promotions/banners/admin/BAN_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bannerId").value("BAN_1"));
    }

    @Test
    void createDefaultsActiveToTrueWhenOmitted() throws Exception {
        when(createBannerInputPort.execute(any(BannerCommands.CreateBanner.class)))
                .thenReturn(sample("BAN_1"));

        mockMvc.perform(post("/api/v1/promotions/banners")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "title": "Summer",
                          "imageUrl": "https://cdn/a.png",
                          "imagePublicId": "aionn/promotion/banners/a",
                          "linkUrl": "https://shop/sale",
                          "displayOrder": 2
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bannerId").value("BAN_1"));

        ArgumentCaptor<BannerCommands.CreateBanner> captor = ArgumentCaptor.forClass(BannerCommands.CreateBanner.class);
        verify(createBannerInputPort).execute(captor.capture());
        assertThat(captor.getValue().active()).isTrue();
    }

    @Test
    void createRejectsMissingImageUrl() throws Exception {
        mockMvc.perform(post("/api/v1/promotions/banners")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "title": "Summer",
                          "imageUrl": "",
                          "imagePublicId": "aionn/promotion/banners/a",
                          "displayOrder": 2
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsMissingImagePublicId() throws Exception {
        mockMvc.perform(post("/api/v1/promotions/banners")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "title": "Summer",
                          "imageUrl": "https://res.cloudinary.com/demo/image/upload/banner.png",
                          "linkUrl": "https://shop/sale",
                          "displayOrder": 2
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateInvokesInputPort() throws Exception {
        when(updateBannerInputPort.execute(any(BannerCommands.UpdateBanner.class)))
                .thenReturn(sample("BAN_1"));

        mockMvc.perform(put("/api/v1/promotions/banners/BAN_1")
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "title": "Winter",
                          "displayOrder": 9,
                          "active": false
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Promotion banner updated"));
    }

    @Test
    void deleteInvokesInputPort() throws Exception {
        mockMvc.perform(delete("/api/v1/promotions/banners/BAN_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Promotion banner deleted"));

        verify(deleteBannerInputPort).execute(new BannerCommands.DeleteBanner("BAN_1"));
    }
}
