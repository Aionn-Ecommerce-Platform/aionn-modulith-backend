package com.aionn.chat.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.chat.domain.event.ChatEvents;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

@Getter
public class MerchantAutoReply extends AggregateRoot {

    private final String merchantId;
    private boolean enabled;
    private String greeting;
    private String awayMessage;
    private LocalTime workingHourStart;
    private LocalTime workingHourEnd;
    private Set<DayOfWeek> workingDays;
    private final ZoneId timezone;
    private final Instant createdAt;
    private Instant updatedAt;

    public MerchantAutoReply(String merchantId, boolean enabled, String greeting, String awayMessage,
            LocalTime workingHourStart, LocalTime workingHourEnd, Set<DayOfWeek> workingDays,
            ZoneId timezone, Instant createdAt, Instant updatedAt) {
        this.merchantId = merchantId;
        this.enabled = enabled;
        this.greeting = greeting;
        this.awayMessage = awayMessage;
        this.workingHourStart = workingHourStart;
        this.workingHourEnd = workingHourEnd;
        this.workingDays = workingDays == null ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(workingDays);
        this.timezone = timezone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MerchantAutoReply create(String merchantId) {
        return create(merchantId, Clock.systemUTC());
    }

    public static MerchantAutoReply create(String merchantId, Clock clock) {
        Instant now = clock.instant();
        Set<DayOfWeek> defaults = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
        return new MerchantAutoReply(merchantId, false, null, null,
                LocalTime.of(8, 0), LocalTime.of(22, 0), defaults, ZoneId.of("Asia/Ho_Chi_Minh"), now, now);
    }

    public void update(boolean enabled, String greeting, String awayMessage,
            LocalTime workingHourStart, LocalTime workingHourEnd, Set<DayOfWeek> workingDays) {
        update(enabled, greeting, awayMessage, workingHourStart, workingHourEnd, workingDays,
                Clock.systemUTC());
    }

    public void update(boolean enabled, String greeting, String awayMessage,
            LocalTime workingHourStart, LocalTime workingHourEnd, Set<DayOfWeek> workingDays,
            Clock clock) {
        Guard.require(workingHourStart == null || workingHourEnd == null
                || workingHourStart.isBefore(workingHourEnd),
                () -> new ChatException(ChatErrorCode.INVALID_ARGUMENT,
                        "workingHourStart must be before workingHourEnd"));
        this.enabled = enabled;
        this.greeting = greeting;
        this.awayMessage = awayMessage;
        if (workingHourStart != null)
            this.workingHourStart = workingHourStart;
        if (workingHourEnd != null)
            this.workingHourEnd = workingHourEnd;
        if (workingDays != null) {
            this.workingDays = workingDays.isEmpty()
                    ? EnumSet.noneOf(DayOfWeek.class)
                    : EnumSet.copyOf(workingDays);
        }
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new ChatEvents.AutoReplyConfigured(merchantId, enabled, now));
    }

    public boolean isWithinWorkingHours(Instant now) {
        OffsetDateTime local = now.atZone(timezone).toOffsetDateTime();
        if (!workingDays.contains(local.getDayOfWeek()))
            return false;
        LocalTime t = local.toLocalTime();
        return !t.isBefore(workingHourStart) && t.isBefore(workingHourEnd);
    }

    @Override
    protected String aggregateId() {
        return merchantId;
    }
}
