package com.aionn.chat.domain.model;

import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantAutoReplyTest {

    private static final String MERCHANT = "merchant-1";
    private static final ZoneId TZ = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void createUsesDefaultsAndIsDisabled() {
        MerchantAutoReply ar = MerchantAutoReply.create(MERCHANT);

        assertThat(ar.getMerchantId()).isEqualTo(MERCHANT);
        assertThat(ar.isEnabled()).isFalse();
        assertThat(ar.getWorkingHourStart()).isEqualTo(LocalTime.of(8, 0));
        assertThat(ar.getWorkingHourEnd()).isEqualTo(LocalTime.of(22, 0));
        assertThat(ar.getWorkingDays()).containsExactlyInAnyOrderElementsOf(
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY));
    }

    @Test
    void updateRejectsStartAfterEnd() {
        MerchantAutoReply ar = MerchantAutoReply.create(MERCHANT);

        assertThatThrownBy(() -> ar.update(true, "hi", "bye",
                LocalTime.of(20, 0), LocalTime.of(8, 0),
                EnumSet.of(DayOfWeek.MONDAY)))
                .isInstanceOf(ChatException.class)
                .extracting("errorCode").isEqualTo(ChatErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void updateAppliesValuesAndRecordsEvent() {
        MerchantAutoReply ar = MerchantAutoReply.create(MERCHANT);

        ar.update(true, "Hello", "Away", LocalTime.of(9, 0), LocalTime.of(18, 0),
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));

        assertThat(ar.isEnabled()).isTrue();
        assertThat(ar.getGreeting()).isEqualTo("Hello");
        assertThat(ar.getAwayMessage()).isEqualTo("Away");
        assertThat(ar.getWorkingHourStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(ar.getWorkingHourEnd()).isEqualTo(LocalTime.of(18, 0));
        assertThat(ar.getWorkingDays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        assertThat(ar.peekEvents()).hasSize(1);
    }

    @Test
    void isWithinWorkingHoursTrueOnWorkingDayInRange() {
        MerchantAutoReply ar = mondayNineToFive();

        Instant noonMonday = ZonedDateTime.of(2025, 1, 6, 12, 0, 0, 0, TZ) // Mon
                .toInstant();

        assertThat(ar.isWithinWorkingHours(noonMonday)).isTrue();
    }

    @Test
    void isWithinWorkingHoursFalseOutsideRange() {
        MerchantAutoReply ar = mondayNineToFive();

        Instant earlyMonday = ZonedDateTime.of(2025, 1, 6, 7, 59, 0, 0, TZ).toInstant();
        Instant lateMonday = ZonedDateTime.of(2025, 1, 6, 17, 1, 0, 0, TZ).toInstant();

        assertThat(ar.isWithinWorkingHours(earlyMonday)).isFalse();
        assertThat(ar.isWithinWorkingHours(lateMonday)).isFalse();
    }

    @Test
    void isWithinWorkingHoursFalseOnNonWorkingDay() {
        MerchantAutoReply ar = mondayNineToFive();
        Instant sundayNoon = ZonedDateTime.of(2025, 1, 5, 12, 0, 0, 0, TZ).toInstant();

        assertThat(ar.isWithinWorkingHours(sundayNoon)).isFalse();
    }

    private static MerchantAutoReply mondayNineToFive() {
        Set<DayOfWeek> mondayOnly = EnumSet.of(DayOfWeek.MONDAY);
        return new MerchantAutoReply(MERCHANT, true, "hi", "away",
                LocalTime.of(9, 0), LocalTime.of(17, 0), mondayOnly,
                TZ, Instant.now(), Instant.now());
    }
}
