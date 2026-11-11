package com.aionn.chat.application.policy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

/** Defaults applied when a merchant has not configured chat auto-reply yet. */
public interface ChatAutoReplyPolicy {

    LocalTime workingHourStart();

    LocalTime workingHourEnd();

    Set<DayOfWeek> workingDays();

    ZoneId timezone();
}
