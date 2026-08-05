package com.aionn.sharedkernel.infrastructure.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class OutboxDeadLetterMetrics {

    OutboxDeadLetterMetrics(MeterRegistry meterRegistry, OutboxEventRepository repository) {
        Gauge.builder("aionn.outbox.dead.letter.count", repository, OutboxEventRepository::countDeadLetters)
                .description("Current number of transactional outbox events awaiting operator recovery")
                .register(meterRegistry);
    }
}
