package com.aionn.sharedkernel.adapter.web.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidJsonValidatorTest {

    private final ValidJsonValidator validator = new ValidJsonValidator();

    @Test
    void nullIsTreatedAsUnsetAndPasses() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void blankIsTreatedAsUnsetAndPasses() {
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void wellFormedObjectPasses() {
        assertThat(validator.isValid("{\"key\":\"value\"}", null)).isTrue();
    }

    @Test
    void wellFormedArrayPasses() {
        assertThat(validator.isValid("[1, 2, 3]", null)).isTrue();
    }

    @Test
    void jsonScalarPasses() {
        // ObjectMapper.readTree accepts primitives as top-level values.
        assertThat(validator.isValid("42", null)).isTrue();
    }

    @Test
    void malformedObjectFails() {
        assertThat(validator.isValid("{invalid", null)).isFalse();
    }

    @Test
    void trailingCommaFails() {
        assertThat(validator.isValid("{\"a\": 1,}", null)).isFalse();
    }

    @Test
    void unquotedKeyFails() {
        assertThat(validator.isValid("{a: 1}", null)).isFalse();
    }
}
