package com.aionn.sharedkernel.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryBackoffTest {

    @Test
    void doublesDelayPerAttempt() {
        assertThat(RetryBackoff.seconds(1)).isEqualTo(2);
        assertThat(RetryBackoff.seconds(3)).isEqualTo(8);
        assertThat(RetryBackoff.seconds(10)).isEqualTo(1024);
    }

    @Test
    void capsDelayAtOneHour() {
        assertThat(RetryBackoff.seconds(12)).isEqualTo(RetryBackoff.MAX_DELAY_SECONDS);
        assertThat(RetryBackoff.seconds(50)).isEqualTo(RetryBackoff.MAX_DELAY_SECONDS);
    }

    @Test
    void treatsNegativeAttemptsAsFirstAttempt() {
        assertThat(RetryBackoff.seconds(-5)).isEqualTo(1);
    }
}
