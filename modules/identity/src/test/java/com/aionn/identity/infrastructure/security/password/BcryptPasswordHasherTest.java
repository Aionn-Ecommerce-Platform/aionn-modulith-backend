package com.aionn.identity.infrastructure.security.password;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(10);

    @Test
    void hashProducesBcryptString() {
        String hashed = hasher.hash("password");
        assertThat(hashed).satisfiesAnyOf(
                h -> assertThat(h).startsWith("$2a$"),
                h -> assertThat(h).startsWith("$2b$"),
                h -> assertThat(h).startsWith("$2y$")
        );
        assertThat(hashed).hasSizeGreaterThanOrEqualTo(60);
    }

    @Test
    void hashIsSaltedProducingDifferentHashesEachTime() {
        assertThat(hasher.hash("password")).isNotEqualTo(hasher.hash("password"));
    }

    @Test
    void matchesAcceptsCorrectPassword() {
        String hashed = hasher.hash("Secret-123");
        assertThat(hasher.matches("Secret-123", hashed)).isTrue();
    }

    @Test
    void matchesRejectsWrongPassword() {
        String hashed = hasher.hash("Secret-123");
        assertThat(hasher.matches("Secret-124", hashed)).isFalse();
    }

    @Test
    void matchesRejectsTamperedHash() {
        String hashed = hasher.hash("password");
        String tampered = hashed.substring(0, hashed.length() - 1) + "X";
        assertThat(hasher.matches("password", tampered)).isFalse();
    }
}
