package com.aionn.identity.domain.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityErrorCodeTest {

    @Test
    void codesAreUnique() {
        var seen = new java.util.HashMap<String, IdentityErrorCode>();
        Arrays.stream(IdentityErrorCode.values()).forEach(code -> {
            IdentityErrorCode previous = seen.put(code.getCode(), code);
            assertThat(previous)
                    .as("duplicate error code %s shared by %s and %s",
                            code.getCode(), previous, code)
                    .isNull();
        });
    }
}
