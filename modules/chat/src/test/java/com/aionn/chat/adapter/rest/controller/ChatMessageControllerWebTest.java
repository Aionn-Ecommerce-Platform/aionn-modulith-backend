package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.exception.ChatExceptionHandler;
import com.aionn.chat.adapter.rest.mapper.message.MessageDtoMapperImpl;
import com.aionn.chat.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.chat.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.application.port.in.message.ListMessagesQueryPort;
import com.aionn.chat.application.port.in.message.MarkDeliveredInputPort;
import com.aionn.chat.application.port.in.message.MarkReadInputPort;
import com.aionn.chat.application.port.in.message.RecallMessageInputPort;
import com.aionn.chat.application.port.in.message.SendMessageInputPort;
import com.aionn.chat.application.port.in.message.SetTypingInputPort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerWebTest {

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private SendMessageInputPort sendMessageInputPort;
        @Mock
        private ListMessagesQueryPort listMessagesQueryPort;
        @Mock
        private MarkDeliveredInputPort markDeliveredInputPort;
        @Mock
        private MarkReadInputPort markReadInputPort;
        @Mock
        private RecallMessageInputPort recallMessageInputPort;
        @Mock
        private SetTypingInputPort setTypingInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                ChatMessageController controller = new ChatMessageController(
                                sendMessageInputPort, listMessagesQueryPort, markDeliveredInputPort,
                                markReadInputPort, recallMessageInputPort, setTypingInputPort,
                                new MessageDtoMapperImpl());

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new ChatExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                                .build();

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "sender-1", "n/a",
                                                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        private static MessageResult sample(String messageId, String status) {
                return new MessageResult(messageId, "conv-1", "sender-1", "BUYER", "TEXT",
                                "Hello", Map.of(), status, Set.of(), Set.of(), false, NOW, NOW);
        }

        @Test
        void sendUsesCurrentUserAsSender() throws Exception {
                when(sendMessageInputPort.execute(any(MessageCommands.SendMessage.class)))
                                .thenReturn(sample("msg-1", "SENT"));

                mockMvc.perform(post("/api/v1/chat/conversations/conv-1/messages")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "type": "TEXT",
                                                  "body": "Hello"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.messageId").value("msg-1"))
                                .andExpect(jsonPath("$.message").value("Message sent"));

                ArgumentCaptor<MessageCommands.SendMessage> captor = ArgumentCaptor
                                .forClass(MessageCommands.SendMessage.class);
                verify(sendMessageInputPort).execute(captor.capture());
                assertThat(captor.getValue().senderId()).isEqualTo("sender-1");
                assertThat(captor.getValue().conversationId()).isEqualTo("conv-1");
        }

        @Test
        void sendRejectsMissingType() throws Exception {
                mockMvc.perform(post("/api/v1/chat/conversations/conv-1/messages")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "body": "Hello"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void listUsesDefaultLimitAndNoCursor() throws Exception {
                when(listMessagesQueryPort.execute(eq("sender-1"), eq("conv-1"), isNull(), eq(30)))
                                .thenReturn(List.of(sample("msg-1", "SENT")));

                mockMvc.perform(get("/api/v1/chat/conversations/conv-1/messages"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].messageId").value("msg-1"));

                verify(listMessagesQueryPort).execute("sender-1", "conv-1", null, 30);
        }

        @Test
        void listPassesCursorThrough() throws Exception {
                when(listMessagesQueryPort.execute("sender-1", "conv-1", NOW, 10))
                                .thenReturn(List.of(sample("msg-0", "SENT")));

                mockMvc.perform(get("/api/v1/chat/conversations/conv-1/messages")
                                .param("before", NOW.toString())
                                .param("limit", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].messageId").value("msg-0"));
        }

        @Test
        void markDeliveredInvokesInputPort() throws Exception {
                when(markDeliveredInputPort.execute(any(MessageCommands.DeliverMessage.class)))
                                .thenReturn(sample("msg-1", "DELIVERED"));

                mockMvc.perform(post("/api/v1/chat/messages/msg-1/delivered"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

                verify(markDeliveredInputPort).execute(
                                new MessageCommands.DeliverMessage("sender-1", "msg-1"));
        }

        @Test
        void markReadInvokesInputPort() throws Exception {
                when(markReadInputPort.execute(any(MessageCommands.ReadMessage.class)))
                                .thenReturn(sample("msg-1", "READ"));

                mockMvc.perform(post("/api/v1/chat/messages/msg-1/read"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("READ"));
        }

        @Test
        void recallReturnsRecalledMessage() throws Exception {
                when(recallMessageInputPort.execute(any(MessageCommands.RecallMessage.class)))
                                .thenReturn(sample("msg-1", "RECALLED"));

                mockMvc.perform(post("/api/v1/chat/messages/msg-1/recall"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("RECALLED"));
        }

        @Test
        void recallAfterWindowReturnsBadRequest() throws Exception {
                when(recallMessageInputPort.execute(any(MessageCommands.RecallMessage.class)))
                                .thenThrow(new ChatException(ChatErrorCode.MESSAGE_RECALL_WINDOW_EXPIRED));

                mockMvc.perform(post("/api/v1/chat/messages/msg-1/recall"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.data.errorCode")
                                                .value(ChatErrorCode.MESSAGE_RECALL_WINDOW_EXPIRED.getCode()));
        }

        @Test
        void setTypingInvokesInputPort() throws Exception {
                mockMvc.perform(post("/api/v1/chat/conversations/conv-1/typing")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "typing": true
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Typing state updated"));

                verify(setTypingInputPort).execute(
                                new MessageCommands.SetTyping("sender-1", "conv-1", true));
        }
}
