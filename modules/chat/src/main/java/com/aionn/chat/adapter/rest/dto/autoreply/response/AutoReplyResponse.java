package com.aionn.chat.adapter.rest.dto.autoreply.response;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

public record AutoReplyResponse(
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
