package com.aionn.chat.infrastructure.persistence.mapper;

import com.aionn.chat.domain.model.MerchantAutoReply;
import com.aionn.chat.infrastructure.persistence.entity.MerchantAutoReplyEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class MerchantAutoReplyDomainMapper {

    public MerchantAutoReply toDomain(MerchantAutoReplyEntity e) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (e.getWorkingDays() != null) {
            for (String d : e.getWorkingDays()) {
                days.add(DayOfWeek.valueOf(d));
            }
        }
        return new MerchantAutoReply(
                e.getMerchantId(),
                e.isEnabled(),
                e.getGreeting(),
                e.getAwayMessage(),
                e.getWorkingHourStart(),
                e.getWorkingHourEnd(),
                days,
                e.getTimezone() == null ? ZoneId.of("Asia/Ho_Chi_Minh") : ZoneId.of(e.getTimezone()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public MerchantAutoReplyEntity toEntity(MerchantAutoReply a, MerchantAutoReplyEntity existing) {
        MerchantAutoReplyEntity entity = existing != null ? existing
                : MerchantAutoReplyEntity.builder()
                        .merchantId(a.getMerchantId())
                        .timezone(a.getTimezone().getId())
                        .build();
        entity.setEnabled(a.isEnabled());
        entity.setGreeting(a.getGreeting());
        entity.setAwayMessage(a.getAwayMessage());
        entity.setWorkingHourStart(a.getWorkingHourStart());
        entity.setWorkingHourEnd(a.getWorkingHourEnd());
        List<String> days = new ArrayList<>();
        for (DayOfWeek d : a.getWorkingDays())
            days.add(d.name());
        entity.setWorkingDays(days);
        entity.setTimezone(a.getTimezone().getId());
        return entity;
    }
}

