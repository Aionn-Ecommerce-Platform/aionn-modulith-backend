package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.exception.ChatExceptionHandler;
import com.aionn.chat.adapter.rest.mapper.media.ChatMediaDtoMapperImpl;
import com.aionn.chat.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.chat.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.chat.application.dto.media.result.UploadSignatureResult;
import com.aionn.chat.application.port.in.media.GenerateChatMediaUploadSignatureInputPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatMediaControllerWebTest {

    @Mock
    private GenerateChatMediaUploadSignatureInputPort generateChatMediaUploadSignatureInputPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChatMediaController controller = new ChatMediaController(
                generateChatMediaUploadSignatureInputPort, new ChatMediaDtoMapperImpl());

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ChatExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-1", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSignedUploadParametersScopedToCurrentUser() throws Exception {
        when(generateChatMediaUploadSignatureInputPort.execute("user-1")).thenReturn(
                new UploadSignatureResult("sig", "1750000000", "api-key", "demo-cloud",
                        "https://api.cloudinary.com/v1_1/demo-cloud/image/upload",
                        "aionn/chat/images/user-1"));

        mockMvc.perform(post("/api/v1/chat/media/upload-signatures/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signature").value("sig"))
                .andExpect(jsonPath("$.data.folder").value("aionn/chat/images/user-1"));

        verify(generateChatMediaUploadSignatureInputPort).execute("user-1");
    }
}
