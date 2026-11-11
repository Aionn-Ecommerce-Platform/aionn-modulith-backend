package com.aionn.sharedkernel.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdGeneratorTest {

    @Test
    void generatesTwentySixCharacterUlids() {
        String first = IdGenerator.ulid();
        String second = IdGenerator.ulid();

        assertThat(first).hasSize(26).isNotEqualTo(second);
        assertThat(IdGenerator.isValid(first)).isTrue();
        assertThat(IdGenerator.isValid(second)).isTrue();
    }

    @Test
    void roundTripsTheTimestamp() {
        long timestamp = 1_784_000_000_000L;

        String ulid = IdGenerator.ulid(timestamp);

        assertThat(IdGenerator.extractTimestamp(ulid)).isEqualTo(timestamp);
    }

    @Test
    void validatesTheAlphabetAndLength() {
        assertThat(IdGenerator.isValid(null)).isFalse();
        assertThat(IdGenerator.isValid("short")).isFalse();
        assertThat(IdGenerator.isValid("0".repeat(26))).isTrue();
        assertThat(IdGenerator.isValid("0".repeat(25) + "U")).isFalse();
        assertThat(IdGenerator.isValid("0".repeat(25) + "u")).isFalse();
        assertThat(IdGenerator.isValid("0".repeat(25) + "!")).isFalse();
    }

    @Test
    void rejectsInvalidValuesWhenExtractingTheTimestamp() {
        assertThatThrownBy(() -> IdGenerator.extractTimestamp(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdGenerator.extractTimestamp("short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdGenerator.extractTimestamp("!".repeat(26)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
