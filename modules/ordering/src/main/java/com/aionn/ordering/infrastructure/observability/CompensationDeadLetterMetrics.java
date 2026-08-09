package com.aionn.ordering.infrastructure.observability;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CompensationDeadLetterMetrics {
    public CompensationDeadLetterMetrics(MeterRegistry registry, CompensationTaskPort tasks) {
        Gauge.builder("aionn.ordering.compensation.dead.letter.count", tasks,
                CompensationTaskPort::countDeadLetters).register(registry);
    }
}
