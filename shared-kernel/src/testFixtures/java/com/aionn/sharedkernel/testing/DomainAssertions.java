package com.aionn.sharedkernel.testing;

import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.domain.vo.Money;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.math.BigDecimal;
import java.util.List;

public final class DomainAssertions {

    private DomainAssertions() {
    }

    public static AggregateAssert assertThat(AggregateRoot aggregate) {
        return new AggregateAssert(aggregate);
    }

    public static MoneyAssert assertThat(Money money) {
        return new MoneyAssert(money);
    }

    public static class AggregateAssert extends AbstractAssert<AggregateAssert, AggregateRoot> {

        private AggregateAssert(AggregateRoot aggregate) {
            super(aggregate, AggregateAssert.class);
        }

        public AggregateAssert hasPublishedEvent(Class<? extends DomainEvent> eventType) {
            isNotNull();
            List<EventEnvelope> events = actual.peekEvents();
            boolean found = events.stream().map(EventEnvelope::payload).anyMatch(eventType::isInstance);
            if (!found) {
                failWithMessage("Expected aggregate to have published event <%s> but it didn't. Published: <%s>",
                        eventType.getSimpleName(),
                        events.stream().map(e -> e.payload().getClass().getSimpleName()).toList());
            }
            return this;
        }

        public AggregateAssert hasNotPublishedEvent(Class<? extends DomainEvent> eventType) {
            isNotNull();
            boolean found = actual.peekEvents().stream().map(EventEnvelope::payload).anyMatch(eventType::isInstance);
            if (found) {
                failWithMessage("Expected aggregate NOT to have published event <%s>, but it did.",
                        eventType.getSimpleName());
            }
            return this;
        }

        public AggregateAssert hasExactlyNEvents(int expectedCount) {
            isNotNull();
            int actual = this.actual.peekEvents().size();
            if (actual != expectedCount) {
                failWithMessage("Expected exactly <%d> domain event(s), but got <%d>.",
                        expectedCount, actual);
            }
            return this;
        }

        public AggregateAssert hasNoPublishedEvents() {
            return hasExactlyNEvents(0);
        }

        public <E extends DomainEvent> E extractEvent(Class<E> eventType) {
            isNotNull();
            return actual.peekEvents().stream()
                    .map(EventEnvelope::payload)
                    .filter(eventType::isInstance)
                    .map(eventType::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No event of type " + eventType.getSimpleName() + " found"));
        }
    }

    public static class MoneyAssert extends AbstractAssert<MoneyAssert, Money> {

        private MoneyAssert(Money money) {
            super(money, MoneyAssert.class);
        }

        public MoneyAssert hasAmount(String expectedAmount) {
            isNotNull();
            BigDecimal expected = new BigDecimal(expectedAmount);
            if (actual.amount().compareTo(expected) != 0) {
                failWithMessage("Expected Money amount to be <%s> but was <%s>",
                        expectedAmount, actual.amount().toPlainString());
            }
            return this;
        }

        public MoneyAssert hasCurrency(String currencyCode) {
            isNotNull();
            Assertions.assertThat(actual.currency())
                    .as("Money currency")
                    .isEqualTo(currencyCode);
            return this;
        }

        public MoneyAssert isZero() {
            isNotNull();
            if (actual.amount().compareTo(BigDecimal.ZERO) != 0) {
                failWithMessage("Expected Money to be zero but was <%s>", actual);
            }
            return this;
        }

        public MoneyAssert isGreaterThan(Money other) {
            isNotNull();
            if (!actual.isGreaterThan(other)) {
                failWithMessage("Expected <%s> to be greater than <%s>", actual, other);
            }
            return this;
        }

        public MoneyAssert isLessThan(Money other) {
            isNotNull();
            if (!actual.isLessThan(other)) {
                failWithMessage("Expected <%s> to be less than <%s>", actual, other);
            }
            return this;
        }
    }
}
