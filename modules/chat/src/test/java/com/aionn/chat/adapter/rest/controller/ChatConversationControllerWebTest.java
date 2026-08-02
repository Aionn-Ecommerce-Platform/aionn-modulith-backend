package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.exception.ChatExceptionHandler;
import com.aionn.chat.adapter.rest.mapper.conversation.ConversationDtoMapperImpl;
import com.aionn.chat.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.chat.adapter.rest.support.session.CurrentUserIdArgumentResolver;
import com.aionn.chat.application.dto.conversation.command.ConversationCommands;
import com.aionn.chat.application.dto.conversation.result.ConversationResult;
import com.aionn.chat.application.port.in.conversation.ArchiveConversationInputPort;
import com.aionn.chat.application.port.in.conversation.GetConversationQueryPort;
import com.aionn.chat.application.port.in.conversation.GetUnreadCountsQueryPort;
import com.aionn.chat.application.port.in.conversation.JoinSupportInputPort;
import com.aionn.chat.application.port.in.conversation.ListMyConversationsQueryPort;
import com.aionn.chat.application.port.in.conversation.MarkConversationReadInputPort;
import com.aionn.chat.application.port.in.conversation.StartConversationInputPort;
import com.aionn.chat.application.port.in.conversation.UnarchiveConversationInputPort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatConversationControllerWebTest {

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private StartConversationInputPort startConversationInputPort;
        @Mock
        private ListMyConversationsQueryPort listMyConversationsQueryPort;
        @Mock
        private GetConversationQueryPort getConversationQueryPort;
        @Mock
        private MarkConversationReadInputPort markConversationReadInputPort;
        @Mock
        private ArchiveConversationInputPort archiveConversationInputPort;
        @Mock
        private UnarchiveConversationInputPort unarchiveConversationInputPort;
        @Mock
        private JoinSupportInputPort joinSupportInputPort;
        @Mock
        private GetUnreadCountsQueryPort getUnreadCountsQueryPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                ChatConversationController controller = new ChatConversationController(
                                startConversationInputPort, listMyConversationsQueryPort, getConversationQueryPort,
                                markConversationReadInputPort, archiveConversationInputPort,
                                unarchiveConversationInputPort, joinSupportInputPort, getUnreadCountsQueryPort,
                                new ConversationDtoMapperImpl());

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new ChatExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                                                JacksonMapperFactory.create()))
                                .setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
                                .build();

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                                "buyer-1", "n/a",
                                                List.of(new SimpleGrantedAuthority("ROLE_USER"),
                                                                new SimpleGrantedAuthority("ROLE_CS_ADMIN"))));
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        private static ConversationResult sample(String conversationId, boolean archived, long unread) {
                return new ConversationResult(conversationId, "buyer-1", "mer-1", List.of(),
                                null, null, null, null, null, archived, unread, NOW, NOW);
        }

        @Test
        void startUsesCurrentUserAsBuyerAndStarter() throws Exception {
                when(startConversationInputPort.execute(any(ConversationCommands.StartConversation.class)))
                                .thenReturn(sample("conv-1", false, 0));

                mockMvc.perform(post("/api/v1/chat/conversations")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "merchantId": "mer-1",
                                                  "buyerDisplayName": "Buyer",
                                                  "merchantDisplayName": "Shop"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.conversationId").value("conv-1"))
                                .andExpect(jsonPath("$.message").value("Conversation ready"));

                ArgumentCaptor<ConversationCommands.StartConversation> captor = ArgumentCaptor
                                .forClass(ConversationCommands.StartConversation.class);
                verify(startConversationInputPort).execute(captor.capture());
                assertThat(captor.getValue().buyerId()).isEqualTo("buyer-1");
                assertThat(captor.getValue().startedBy()).isEqualTo("buyer-1");
        }

        @Test
        void startRejectsBlankMerchantId() throws Exception {
                mockMvc.perform(post("/api/v1/chat/conversations")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "merchantId": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void listMineUsesDefaults() throws Exception {
                when(listMyConversationsQueryPort.execute("buyer-1", false, 50))
                                .thenReturn(List.of(sample("conv-1", false, 2)));

                mockMvc.perform(get("/api/v1/chat/conversations"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].conversationId").value("conv-1"))
                                .andExpect(jsonPath("$.data[0].unreadCount").value(2));

                verify(listMyConversationsQueryPort).execute("buyer-1", false, 50);
        }

        @Test
        void listMinePassesRawLimitToUseCase() throws Exception {
                when(listMyConversationsQueryPort.execute("buyer-1", true, 9999)).thenReturn(List.of());

                mockMvc.perform(get("/api/v1/chat/conversations")
                                .param("includeArchived", "true")
                                .param("limit", "9999"))
                                .andExpect(status().isOk());

                verify(listMyConversationsQueryPort).execute("buyer-1", true, 9999);
        }

        @Test
        void getReturnsConversation() throws Exception {
                when(getConversationQueryPort.execute("buyer-1", "conv-1"))
                                .thenReturn(sample("conv-1", false, 0));

                mockMvc.perform(get("/api/v1/chat/conversations/conv-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.conversationId").value("conv-1"));
        }

        @Test
        void getNotFoundWhenMissing() throws Exception {
                when(getConversationQueryPort.execute("buyer-1", "nope"))
                                .thenThrow(new ChatException(ChatErrorCode.CONVERSATION_NOT_FOUND));

                mockMvc.perform(get("/api/v1/chat/conversations/nope"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.data.errorCode")
                                                .value(ChatErrorCode.CONVERSATION_NOT_FOUND.getCode()));
        }

        @Test
        void markReadInvokesInputPort() throws Exception {
                when(markConversationReadInputPort.execute(any(ConversationCommands.MarkRead.class)))
                                .thenReturn(sample("conv-1", false, 0));

                mockMvc.perform(post("/api/v1/chat/conversations/conv-1/read"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Conversation marked read"));

                verify(markConversationReadInputPort).execute(
                                new ConversationCommands.MarkRead("buyer-1", "conv-1"));
        }

        @Test
        void archiveInvokesInputPort() throws Exception {
                when(archiveConversationInputPort.execute(any(ConversationCommands.Archive.class)))
                                .thenReturn(sample("conv-1", true, 0));

                mockMvc.perform(post("/api/v1/chat/conversations/conv-1/archive"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.archived").value(true));
        }

        @Test
        void unarchiveInvokesInputPort() throws Exception {
                when(unarchiveConversationInputPort.execute(any(ConversationCommands.Unarchive.class)))
                                .thenReturn(sample("conv-1", false, 0));

                mockMvc.perform(post("/api/v1/chat/conversations/conv-1/unarchive"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.archived").value(false));
        }

        @Test
        void joinSupportToleratesMissingBody() throws Exception {
                when(joinSupportInputPort.execute(any(ConversationCommands.JoinSupport.class)))
                                .thenReturn(sample("conv-1", false, 0));

                mockMvc.perform(post("/api/v1/chat/conversations/conv-1/support"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Support joined"));

                ArgumentCaptor<ConversationCommands.JoinSupport> captor = ArgumentCaptor
                                .forClass(ConversationCommands.JoinSupport.class);
                verify(joinSupportInputPort).execute(captor.capture());
                assertThat(captor.getValue().supportUserId()).isEqualTo("buyer-1");
                assertThat(captor.getValue().displayName()).isNull();
        }

        @Test
        void unreadCountsReturnsMap() throws Exception {
                when(getUnreadCountsQueryPort.execute("buyer-1")).thenReturn(Map.of("conv-1", 3L));

                mockMvc.perform(get("/api/v1/chat/conversations/unread-counts"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data['conv-1']").value(3));
        }
}
