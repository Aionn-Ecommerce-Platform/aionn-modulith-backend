package com.aionn.chat.application.dto.autoreply.result;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

public record AutoReplyResult(
        String merchantId,
        boolean enabled,
        String greeting,
        String awayMessage,
        LocalTime workingHourStart,
        LocalTime workingHourEnd,
        Set<DayOfWeek> workingDays,
        String timezone,
        Instant createdAt,
        Instant updatedAt) {
}

