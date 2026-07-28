package com.aionn.chat.infrastructure.policy;

import com.aionn.chat.infrastructure.config.properties.ChatAutoReplyProperties;
import com.aionn.chat.infrastructure.config.properties.ChatConversationProperties;
import com.aionn.chat.infrastructure.config.properties.ChatMessageProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPolicyTest {

    @Test
    void messagePolicyExposesConfiguredLimits() {
        SpringChatMessagePolicy policy = new SpringChatMessagePolicy(
                new ChatMessageProperties(4000, 120, 30, 100));

        assertThat(policy.getMaxTextLength()).isEqualTo(4000);
        assertThat(policy.getRecallWindow()).isEqualTo(Duration.ofSeconds(120));
        assertThat(policy.getListDefaultLimit()).isEqualTo(30);
        assertThat(policy.getListMaxLimit()).isEqualTo(100);
    }

    @Test
    void conversationPolicyExposesConfiguredLimits() {
        SpringChatConversationPolicy policy = new SpringChatConversationPolicy(
                new ChatConversationProperties(50, 100));

        assertThat(policy.getListDefaultLimit()).isEqualTo(50);
        assertThat(policy.getListMaxLimit()).isEqualTo(100);
    }

    @Test
    void autoReplyPolicyParsesTimezone() {
        SpringChatAutoReplyPolicy policy = new SpringChatAutoReplyPolicy(
                new ChatAutoReplyProperties("Away now", "Asia/Ho_Chi_Minh"));

        assertThat(policy.getDefaultAwayMessage()).isEqualTo("Away now");
        assertThat(policy.getDefaultTimezone()).isEqualTo(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
