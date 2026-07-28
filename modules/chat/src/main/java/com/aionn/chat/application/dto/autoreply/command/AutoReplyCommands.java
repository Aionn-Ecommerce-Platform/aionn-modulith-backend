package com.aionn.chat.application.dto.autoreply.command;

import com.aionn.sharedkernel.application.command.Command;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public final class AutoReplyCommands {

    private AutoReplyCommands() {
    }

    public record UpdateAutoReply(
            String ownerId,
            String merchantId,
            boolean enabled,
            String greeting,
            String awayMessage,
            LocalTime workingHourStart,
            LocalTime workingHourEnd,
            Set<DayOfWeek> workingDays) implements Command {
    }
}
