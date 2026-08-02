package com.aionn.chat.infrastructure.config;

import com.aionn.chat.application.port.out.ConversationPersistencePort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.model.Conversation;
import com.aionn.chat.infrastructure.realtime.StompPrincipalResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(ChatWebSocketProperties.class)
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    private static final String CONVERSATION_TOPIC_PREFIX = "/topic/conversations/";

    private final StompPrincipalResolver principalResolver;
    private final ConversationPersistencePort conversationRepository;
    private final ChatWebSocketProperties properties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = properties.allowedOrigins().toArray(String[]::new);
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(origins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] { properties.serverHeartbeatMs(), properties.clientHeartbeatMs() })
                .setTaskScheduler(stompHeartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new AuthInterceptor(), new SubscribeAclInterceptor());
    }

    private final class AuthInterceptor implements ChannelInterceptor {
        @Override
        @Nullable
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) {
                return message;
            }
            if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
                return message;
            }
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            String token = authHeaders == null || authHeaders.isEmpty() ? null : authHeaders.get(0);
            String userId = principalResolver.resolveUserId(token);
            if (userId == null) {
                log.warn("STOMP CONNECT rejected - no resolvable bearer token");
                return null;
            }
            Principal principal = new UsernamePasswordAuthenticationToken(userId, null, grantedAuthorities());
            accessor.setUser(principal);
            return message;
        }
    }

    private final class SubscribeAclInterceptor implements ChannelInterceptor {
        @Override
        @Nullable
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                return message;
            }
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith(CONVERSATION_TOPIC_PREFIX)) {
                return message;
            }
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new ChatException(ChatErrorCode.CONVERSATION_FORBIDDEN);
            }
            String conversationId = destination.substring(CONVERSATION_TOPIC_PREFIX.length());
            Optional<Conversation> conversation = conversationRepository.findById(conversationId);
            if (conversation.isEmpty()) {
                throw new ChatException(ChatErrorCode.CONVERSATION_NOT_FOUND);
            }
            conversation.get().requireParticipant(principal.getName());
            return message;
        }
    }

    private static Collection<GrantedAuthority> grantedAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private ThreadPoolTaskScheduler stompHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
