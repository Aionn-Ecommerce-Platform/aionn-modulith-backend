package com.aionn.identity.infrastructure.security;

import com.aionn.identity.infrastructure.security.password.BcryptPasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptPasswordHasherTest {

    @Test
    void hashedValueMatchesOriginalPassword() {
        BcryptPasswordHasher hasher = new BcryptPasswordHasher(10);
        String raw = "Sup3rSecret!";

        String hash = hasher.hash(raw);

        assertNotNull(hash);
        assertNotEquals(raw, hash);
        assertTrue(hasher.matches(raw, hash));
    }

    @Test
    void mismatchReturnsFalse() {
        BcryptPasswordHasher hasher = new BcryptPasswordHasher(10);
        String hash = hasher.hash("correct-password");

        assertFalse(hasher.matches("wrong-password", hash));
    }

    @Test
    void invalidStrengthFallsBackToDefault() {
        BcryptPasswordHasher hasher = new BcryptPasswordHasher(2);
        String hash = hasher.hash("Sup3rSecret!");

        assertNotNull(hash);
        assertTrue(hasher.matches("Sup3rSecret!", hash));
    }
}
