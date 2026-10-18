package com.aionn.ordering.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSnGeneratorTest {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);

    @Test
    void generate_withNonNullInstant_returnsFormattedDateAndRandomSuffix() {
        Instant now = Instant.parse("2026-10-18T12:34:56Z");
        String sn = OrderSnGenerator.generate(now);

        assertThat(sn).isNotNull();
        assertThat(sn).hasSize(14);
        assertThat(sn).startsWith(DATE_FMT.format(now));
        assertThat(sn).matches("^[0-9]{6}[2-9A-HJ-NP-Z]{8}$");
    }

    @Test
    void generate_withNullInstant_fallsBackToEpoch() {
        String sn = OrderSnGenerator.generate(null);

        assertThat(sn).isNotNull();
        assertThat(sn).hasSize(14);
        assertThat(sn).startsWith("700101");
        assertThat(sn).matches("^[0-9]{6}[2-9A-HJ-NP-Z]{8}$");
    }

    @Test
    void generate_consecutiveCallsGenerateDistinctValues() {
        Instant now = Instant.parse("2026-10-18T12:34:56Z");
        String sn1 = OrderSnGenerator.generate(now);
        String sn2 = OrderSnGenerator.generate(now);

        assertThat(sn1).isNotEqualTo(sn2);
    }
}

