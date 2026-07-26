package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserVoucherTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Money AMOUNT = Money.of(new BigDecimal("50000"), "VND");

    private static UserVoucher claimed() {
        return UserVoucher.claim("uv-1", "V-1", "user-1", CLOCK);
    }

    @Test
    void claimStartsInClaimedStateAndRecordsEvent() {
        UserVoucher uv = claimed();

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.CLAIMED);
        assertThat(uv.getClaimedAt()).isEqualTo(NOW);
        assertThat(uv.getUpdatedAt()).isEqualTo(NOW);
        assertThat(uv.pullEvents()).hasSize(1);
    }

    @Test
    void claimWithoutClockUsesSystemTime() {
        UserVoucher uv = UserVoucher.claim("uv-2", "V-2", "user-2");

        assertThat(uv.getClaimedAt()).isNotNull();
        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.CLAIMED);
    }

    @Test
    void reserveMovesToReservedAndStoresOrder() {
        UserVoucher uv = claimed();
        uv.pullEvents();

        uv.reserve("order-1", NOW.plusSeconds(900), CLOCK);

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.RESERVED);
        assertThat(uv.getReservedOrderId()).isEqualTo("order-1");
        assertThat(uv.getReservedAt()).isEqualTo(NOW);
        assertThat(uv.getReservedExpiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(uv.pullEvents()).hasSize(1);
    }

    @Test
    void reserveWithoutClockUsesSystemTime() {
        UserVoucher uv = claimed();

        uv.reserve("order-1", NOW.plusSeconds(900));

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.RESERVED);
    }

    @Test
    void applyRequiresReservedState() {
        UserVoucher uv = claimed();

        assertThatThrownBy(() -> uv.apply(AMOUNT, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void applyMovesToApplied() {
        UserVoucher uv = claimed();
        uv.reserve("order-1", NOW.plusSeconds(900), CLOCK);
        uv.pullEvents();

        uv.apply(AMOUNT, CLOCK);

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.APPLIED);
        assertThat(uv.getAppliedAmount()).isEqualTo(AMOUNT);
        assertThat(uv.getAppliedAt()).isEqualTo(NOW);
        assertThat(uv.pullEvents()).hasSize(1);
    }

    @Test
    void applyWithoutClockUsesSystemTime() {
        UserVoucher uv = claimed();
        uv.reserve("order-1", NOW.plusSeconds(900), CLOCK);

        uv.apply(AMOUNT);

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.APPLIED);
    }

    @Test
    void releaseClearsReservationFields() {
        UserVoucher uv = claimed();
        uv.reserve("order-1", NOW.plusSeconds(900), CLOCK);
        uv.pullEvents();

        uv.release("buyer cancelled", CLOCK);

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.RELEASED);
        assertThat(uv.getReservedOrderId()).isNull();
        assertThat(uv.getReservedAt()).isNull();
        assertThat(uv.getReservedExpiresAt()).isNull();
        assertThat(uv.getReleasedAt()).isEqualTo(NOW);
        assertThat(uv.pullEvents()).hasSize(1);
    }

    @Test
    void releaseRejectsNonReserved() {
        UserVoucher uv = claimed();

        assertThatThrownBy(() -> uv.release("nope", CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void releaseWithoutClockUsesSystemTime() {
        UserVoucher uv = claimed();
        uv.reserve("order-1", NOW.plusSeconds(900), CLOCK);

        uv.release("expired");

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.RELEASED);
    }

    @Test
    void expireMovesClaimedToExpired() {
        UserVoucher uv = claimed();

        uv.expire(CLOCK);

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.EXPIRED);
        assertThat(uv.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void expireIsNoOpForAppliedAndAlreadyExpired() {
        UserVoucher applied = claimed();
        applied.reserve("order-1", NOW.plusSeconds(900), CLOCK);
        applied.apply(AMOUNT, CLOCK);
        applied.expire(CLOCK);
        assertThat(applied.getStatus()).isEqualTo(UserVoucherStatus.APPLIED);

        UserVoucher expired = claimed();
        expired.expire(CLOCK);
        expired.expire(CLOCK);
        assertThat(expired.getStatus()).isEqualTo(UserVoucherStatus.EXPIRED);
    }

    @Test
    void expireWithoutClockUsesSystemTime() {
        UserVoucher uv = claimed();

        uv.expire();

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.EXPIRED);
    }

    @Test
    void isReservationExpiredOnlyWhenReservedAndPastDeadline() {
        UserVoucher uv = claimed();
        assertThat(uv.isReservationExpired(NOW.plusSeconds(10_000))).isFalse();

        uv.reserve("order-1", NOW.plusSeconds(900), CLOCK);
        assertThat(uv.isReservationExpired(NOW.plusSeconds(100))).isFalse();
        assertThat(uv.isReservationExpired(NOW.plusSeconds(1_000))).isTrue();
    }
}
