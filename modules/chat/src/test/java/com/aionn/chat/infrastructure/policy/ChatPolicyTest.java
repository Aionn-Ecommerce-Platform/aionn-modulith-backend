package com.aionn.chat.infrastructure.policy;

import com.aionn.chat.infrastructure.config.properties.ChatConversationProperties;
import com.aionn.chat.infrastructure.config.properties.ChatAutoReplyProperties;
import com.aionn.chat.infrastructure.config.properties.ChatMessageProperties;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPolicyTest {

    @Test
    void messagePolicyExposesConfiguredLimits() {
        SpringChatMessagePolicy policy = new SpringChatMessagePolicy(
                new ChatMessageProperties(4000, 120, 30, 100));

        assertThat(policy.maxTextLength()).isEqualTo(4000);
        assertThat(policy.recallWindow()).isEqualTo(Duration.ofSeconds(120));
        assertThat(policy.listDefaultLimit()).isEqualTo(30);
        assertThat(policy.listMaxLimit()).isEqualTo(100);
    }

    @Test
    void conversationPolicyExposesConfiguredLimits() {
        SpringChatConversationPolicy policy = new SpringChatConversationPolicy(
                new ChatConversationProperties(50, 100, 100));

        assertThat(policy.listDefaultLimit()).isEqualTo(50);
        assertThat(policy.listMaxLimit()).isEqualTo(100);
        assertThat(policy.unreadScanLimit()).isEqualTo(100);
    }

    @Test
    void autoReplyPolicyExposesConfiguredDefaults() {
        SpringChatAutoReplyPolicy policy = new SpringChatAutoReplyPolicy(
                new ChatAutoReplyProperties(LocalTime.of(8, 0), LocalTime.of(22, 0),
                        EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY),
                        "Asia/Ho_Chi_Minh"));

        assertThat(policy.workingHourStart()).isEqualTo(LocalTime.of(8, 0));
        assertThat(policy.workingHourEnd()).isEqualTo(LocalTime.of(22, 0));
        assertThat(policy.workingDays())
                .containsExactlyInAnyOrderElementsOf(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY));
        assertThat(policy.timezone()).isEqualTo(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
