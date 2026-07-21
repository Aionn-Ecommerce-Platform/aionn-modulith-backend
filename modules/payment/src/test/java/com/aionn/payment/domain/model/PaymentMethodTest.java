package com.aionn.payment.domain.model;

import com.aionn.payment.domain.event.PaymentMethodEvents;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void linkInitializesAsLinkedAndEmitsEvent() {
        PaymentMethod m = PaymentMethod.link("m1", "user-1", "stripe", "4242", "tok-abc", FIXED_NOW);

        assertThat(m.getStatus()).isEqualTo(PaymentMethodStatus.LINKED);
        assertThat(m.peekEvents()).anyMatch(env -> env.payload() instanceof PaymentMethodEvents.PaymentMethodLinked);
    }

    @Test
    void linkRejectsBlankToken() {
        assertThatThrownBy(() -> PaymentMethod.link("m1", "user-1", "stripe", "4242", " ", FIXED_NOW))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void verifyTransitionsToVerified() {
        PaymentMethod m = PaymentMethod.link("m1", "user-1", "stripe", "4242", "tok-abc", FIXED_NOW);
        m.pullEvents();

        m.verify(FIXED_NOW);

        assertThat(m.getStatus()).isEqualTo(PaymentMethodStatus.VERIFIED);
        assertThat(m.getVerifiedAt()).isNotNull();
        assertThat(m.peekEvents()).anyMatch(env -> env.payload() instanceof PaymentMethodEvents.PaymentMethodVerified);
    }

    @Test
    void removeIsIdempotent() {
        PaymentMethod m = PaymentMethod.link("m1", "user-1", "stripe", "4242", "tok-abc", FIXED_NOW);
        m.remove(FIXED_NOW);
        m.pullEvents();

        m.remove(FIXED_NOW);

        assertThat(m.getStatus()).isEqualTo(PaymentMethodStatus.REMOVED);
        assertThat(m.peekEvents()).isEmpty();
    }

    @Test
    void verifyAfterRemovedThrows() {
        PaymentMethod m = PaymentMethod.link("m1", "user-1", "stripe", "4242", "tok-abc", FIXED_NOW);
        m.remove(FIXED_NOW);

        assertThatThrownBy(() -> m.verify(FIXED_NOW))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_INVALID_STATE.getCode());
    }

    @Test
    void ensureOwnedByOtherUserForbidden() {
        PaymentMethod m = PaymentMethod.link("m1", "user-1", "stripe", "4242", "tok-abc", FIXED_NOW);

        assertThatThrownBy(() -> m.ensureOwnedBy("OTHER"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_FORBIDDEN.getCode());
    }
}
