package com.aionn.promotion.infrastructure.scheduling;

import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VoucherAutoReleaseWorkerTest {

    private static final Instant NOW = Instant.parse("2026-08-05T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Money DISCOUNT = Money.of(BigDecimal.valueOf(50_000), "VND");

    private final VoucherPersistencePort voucherRepository = mock(VoucherPersistencePort.class);
    private final UserVoucherPersistencePort userVoucherRepository = mock(UserVoucherPersistencePort.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);

    private final VoucherAutoReleaseWorker worker = new VoucherAutoReleaseWorker(
            voucherRepository, userVoucherRepository, eventPublisher, CLOCK);

    private static Voucher reservedVoucher() {
        Voucher voucher = Voucher.issue("V-1", "CAMP_1", DISCOUNT, 10, null, null, CLOCK);
        voucher.reserveSlot(CLOCK);
        return voucher;
    }

    private static UserVoucher reservedUserVoucher() {
        UserVoucher uv = UserVoucher.claim("UV-1", "V-1", "user-1", CLOCK);
        uv.reserve("order-1", NOW.plusSeconds(900), CLOCK);
        uv.pullEvents();
        return uv;
    }

    @Test
    void releasesTheReservationAndReturnsTheVoucherSlot() {
        UserVoucher uv = reservedUserVoucher();
        Voucher voucher = reservedVoucher();
        when(userVoucherRepository.findById("UV-1")).thenReturn(Optional.of(uv));
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(voucher));

        boolean released = worker.releaseOne("UV-1");

        assertThat(released).isTrue();
        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.RELEASED);
        assertThat(uv.getReservedOrderId()).isNull();
        assertThat(voucher.getReservedCount()).isZero();
        assertThat(uv.getReleasedAt()).isEqualTo(NOW);
        // The release timestamp comes from the injected clock, not the system clock,
        // so a fixed-clock test can assert it.
        assertThat(uv.getUpdatedAt()).isEqualTo(NOW);
        verify(voucherRepository).save(voucher);
        verify(userVoucherRepository).save(uv);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void skipsAUserVoucherThatNoLongerExists() {
        when(userVoucherRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(worker.releaseOne("missing")).isFalse();

        verifyNoInteractions(voucherRepository, eventPublisher);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    void skipsAUserVoucherThatHoldsNoReservation() {
        UserVoucher claimedOnly = UserVoucher.claim("UV-2", "V-1", "user-1", CLOCK);
        when(userVoucherRepository.findById("UV-2")).thenReturn(Optional.of(claimedOnly));

        assertThat(worker.releaseOne("UV-2")).isFalse();

        verifyNoInteractions(voucherRepository, eventPublisher);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    void stillReleasesTheUserVoucherWhenTheVoucherRowIsGone() {
        UserVoucher uv = reservedUserVoucher();
        when(userVoucherRepository.findById("UV-1")).thenReturn(Optional.of(uv));
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.empty());

        assertThat(worker.releaseOne("UV-1")).isTrue();

        assertThat(uv.getStatus()).isEqualTo(UserVoucherStatus.RELEASED);
        verify(voucherRepository, never()).save(any());
        verify(userVoucherRepository).save(uv);
    }
}
