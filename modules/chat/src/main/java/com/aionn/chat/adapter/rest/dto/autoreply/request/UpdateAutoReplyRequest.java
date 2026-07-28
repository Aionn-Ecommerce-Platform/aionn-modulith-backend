package com.aionn.chat.adapter.rest.dto.autoreply.request;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record UpdateAutoReplyRequest(
                boolean enabled,
                String greeting,
                String awayMessage,
                LocalTime workingHourStart,
                LocalTime workingHourEnd,
                Set<DayOfWeek> workingDays) {
}
