package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.exception.ChatExceptionHandler;
import com.aionn.chat.adapter.rest.mapper.autoreply.AutoReplyDtoMapperImpl;
import com.aionn.chat.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.chat.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.chat.application.dto.autoreply.command.AutoReplyCommands;
import com.aionn.chat.application.dto.autoreply.result.AutoReplyResult;
import com.aionn.chat.application.port.in.autoreply.GetAutoReplyQueryPort;
import com.aionn.chat.application.port.in.autoreply.UpdateAutoReplyInputPort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AutoReplyControllerWebTest {

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private GetAutoReplyQueryPort getAutoReplyQueryPort;
        @Mock
        private UpdateAutoReplyInputPort updateAutoReplyInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                AutoReplyController controller = new AutoReplyController(
                                getAutoReplyQueryPort, updateAutoReplyInputPort, new AutoReplyDtoMapperImpl());

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new ChatExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                                                JsonMapper.builder().build()))
                                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                                .build();

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "owner-1", "n/a",
                                                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))));
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        private static AutoReplyResult sample(boolean enabled) {
                return new AutoReplyResult("mer-1", enabled, "Hi", "Away",
                                LocalTime.of(8, 0), LocalTime.of(22, 0),
                                Set.of(DayOfWeek.MONDAY), "Asia/Ho_Chi_Minh", NOW, NOW);
        }

        @Test
        void getReturnsAutoReplyConfig() throws Exception {
                when(getAutoReplyQueryPort.execute("owner-1", "mer-1")).thenReturn(sample(false));

                mockMvc.perform(get("/api/v1/chat/merchants/mer-1/auto-reply"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.merchantId").value("mer-1"))
                                .andExpect(jsonPath("$.data.enabled").value(false))
                                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"));
        }

        @Test
        void getForbiddenWhenCallerDoesNotOwnMerchant() throws Exception {
                when(getAutoReplyQueryPort.execute("owner-1", "mer-2"))
                                .thenThrow(new ChatException(ChatErrorCode.AUTO_REPLY_FORBIDDEN));

                mockMvc.perform(get("/api/v1/chat/merchants/mer-2/auto-reply"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.data.errorCode")
                                                .value(ChatErrorCode.AUTO_REPLY_FORBIDDEN.getCode()));
        }

        @Test
        void updateBuildsCommandFromPathAndCurrentUser() throws Exception {
                when(updateAutoReplyInputPort.execute(any(AutoReplyCommands.UpdateAutoReply.class)))
                                .thenReturn(sample(true));

                mockMvc.perform(put("/api/v1/chat/merchants/mer-1/auto-reply")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "enabled": true,
                                                  "greeting": "Hi",
                                                  "awayMessage": "Away",
                                                  "workingHourStart": "08:00:00",
                                                  "workingHourEnd": "22:00:00",
                                                  "workingDays": ["MONDAY"]
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.enabled").value(true))
                                .andExpect(jsonPath("$.message").value("Auto-reply config saved"));

                ArgumentCaptor<AutoReplyCommands.UpdateAutoReply> captor = ArgumentCaptor
                                .forClass(AutoReplyCommands.UpdateAutoReply.class);
                verify(updateAutoReplyInputPort).execute(captor.capture());
                assertThat(captor.getValue().ownerId()).isEqualTo("owner-1");
                assertThat(captor.getValue().merchantId()).isEqualTo("mer-1");
                assertThat(captor.getValue().workingDays()).containsExactly(DayOfWeek.MONDAY);
        }
}
