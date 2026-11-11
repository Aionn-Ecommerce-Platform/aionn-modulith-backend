package com.aionn.chat.infrastructure.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "chat.auto-reply")
public record ChatAutoReplyProperties(
        @NotNull @DefaultValue("08:00") LocalTime workingHourStart,
        @NotNull @DefaultValue("22:00") LocalTime workingHourEnd,
        @NotNull @DefaultValue({"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"})
        Set<DayOfWeek> workingDays,
        @DefaultValue("Asia/Ho_Chi_Minh") String timezone) {
}
