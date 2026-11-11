package com.aionn.chat.infrastructure.policy;

import com.aionn.chat.application.policy.ChatAutoReplyPolicy;
import com.aionn.chat.infrastructure.config.properties.ChatAutoReplyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SpringChatAutoReplyPolicy implements ChatAutoReplyPolicy {

    private final ChatAutoReplyProperties properties;

    @Override
    public LocalTime workingHourStart() {
        return properties.workingHourStart();
    }

    @Override
    public LocalTime workingHourEnd() {
        return properties.workingHourEnd();
    }

    @Override
    public Set<DayOfWeek> workingDays() {
        return properties.workingDays();
    }

    @Override
    public ZoneId timezone() {
        return ZoneId.of(properties.timezone());
    }
}
