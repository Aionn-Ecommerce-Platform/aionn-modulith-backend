package com.aionn.ordering.infrastructure.observability;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompensationDeadLetterMetricsTest {

    @Test
    void exposesTheCurrentDeadLetterCount() {
        var meterRegistry = new SimpleMeterRegistry();
        var tasks = mock(CompensationTaskPort.class);
        when(tasks.countDeadLetters()).thenReturn(3L);

        new CompensationDeadLetterMetrics(meterRegistry, tasks);

        assertThat(meterRegistry.get("aionn.ordering.compensation.dead.letter.count").gauge().value())
                .isEqualTo(3.0);
    }

    @Test
    void reflectsTheCountAtScrapeTimeRatherThanConstructionTime() {
        var meterRegistry = new SimpleMeterRegistry();
        var tasks = mock(CompensationTaskPort.class);
        when(tasks.countDeadLetters()).thenReturn(0L);

        new CompensationDeadLetterMetrics(meterRegistry, tasks);
        var gauge = meterRegistry.get("aionn.ordering.compensation.dead.letter.count").gauge();
        assertThat(gauge.value()).isZero();

        when(tasks.countDeadLetters()).thenReturn(5L);

        assertThat(gauge.value()).isEqualTo(5.0);
    }
}
