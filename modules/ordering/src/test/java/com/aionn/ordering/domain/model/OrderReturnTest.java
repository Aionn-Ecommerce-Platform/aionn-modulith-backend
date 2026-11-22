package com.aionn.ordering.domain.model;

import com.aionn.ordering.domain.event.ReturnEvents;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.valueobject.ReturnReasonType;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderReturnTest {

    private final Instant now = Instant.now();

    @Test
    void requestEmitsReturnRequestedEvent() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "user-1", "merch-1", ReturnReasonType.OTHER, "broken", "img://1", now);

        assertThat(r.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
        assertThat(r.peekEvents()).anyMatch(env -> env.payload() instanceof ReturnEvents.ReturnRequested);
    }

    @Test
    void requestRejectsMissingReasonType() {
        assertThatThrownBy(() -> OrderReturn.request("ret-1", "ord-1", "u", "m", null, "broken", null, now))
                .isInstanceOf(OrderingException.class);
    }

    @Test
    void requestRejectsBlankReason() {
        assertThatThrownBy(() -> OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, " ", null, now))
                .isInstanceOf(OrderingException.class);
    }

    @Test
    void approveRequiresRefundAmount() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", null, now);

        assertThatThrownBy(() -> r.approve(null, "wh-1", now))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void cancelTransitionsRequestedReturnToCancelled() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", "img://1", now);

        r.cancel(now);

        assertThat(r.getStatus()).isEqualTo(ReturnStatus.CANCELLED);
        assertThat(r.getDecidedAt()).isNotNull();
        assertThat(r.peekEvents()).anyMatch(env -> env.payload() instanceof ReturnEvents.ReturnCancelled);
    }

    @Test
    void cancelRejectsAnApprovedReturn() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", "img://1", now);
        r.approve(Money.of(new BigDecimal("100000"), "VND"), "wh-1", now);

        assertThatThrownBy(() -> r.cancel(now))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.RETURN_INVALID_STATE.getCode());
    }

    @Test
    void approveTransitionsToApproved() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", null, now);

        r.approve(Money.of(new BigDecimal("100000"), "VND"), "wh-1", now);

        assertThat(r.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        assertThat(r.getDecidedAt()).isNotNull();
    }

    @Test
    void confirmReceivedAfterApproved() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", null, now);
        r.approve(Money.of(new BigDecimal("100000"), "VND"), "wh-1", now);

        r.confirmReceived("ok", now);

        assertThat(r.getStatus()).isEqualTo(ReturnStatus.ITEM_RECEIVED);
        assertThat(r.getReceivedAt()).isNotNull();
    }

    @Test
    void rejectFromRequestedTransitions() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", null, now);

        r.reject("not eligible", now);

        assertThat(r.getStatus()).isEqualTo(ReturnStatus.REJECTED);
    }

    @Test
    void invalidTransitionFromTerminalRejected() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", null, now);
        r.reject("nope", now);

        assertThatThrownBy(() -> r.approve(Money.of(BigDecimal.ONE, "VND"), null, now))
                .isInstanceOf(OrderingException.class)
                .extracting("errorCode")
                .isEqualTo(OrderingErrorCode.RETURN_INVALID_STATE.getCode());
    }

    @Test
    void evidenceUrlsReturnsEmptyListWhenEvidenceIsMissingOrBlank() {
        OrderReturn missing = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken", null, now);
        OrderReturn blank = OrderReturn.request("ret-2", "ord-2", "u", "m", ReturnReasonType.OTHER, "broken", " ", now);

        assertThat(missing.evidenceUrls()).isEmpty();
        assertThat(blank.evidenceUrls()).isEmpty();
    }

    @Test
    void evidenceUrlsSplitsStoredUrlsAndDropsEmptyEntries() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", ReturnReasonType.OTHER, "broken",
                "https://cdn.example.com/a.jpg\n\nhttps://cdn.example.com/b.mp4\n", now);

        assertThat(r.evidenceUrls()).containsExactly(
                "https://cdn.example.com/a.jpg",
                "https://cdn.example.com/b.mp4");
    }
}