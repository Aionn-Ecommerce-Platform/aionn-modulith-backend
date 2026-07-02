package com.aionn.identity.infrastructure.security.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(10);

    @Test
    void hashProducesBcryptString() {
        String hashed = hasher.hash("password");
        assertTrue(hashed.startsWith("$2a$") || hashed.startsWith("$2b$") || hashed.startsWith("$2y$"));
        assertTrue(hashed.length() >= 60);
    }

    @Test
    void hashIsSaltedProducingDifferentHashesEachTime() {
        assertNotEquals(hasher.hash("password"), hasher.hash("password"));
    }

    @Test
    void matchesAcceptsCorrectPassword() {
        String hashed = hasher.hash("Secret-123");
        assertTrue(hasher.matches("Secret-123", hashed));
    }

    @Test
    void matchesRejectsWrongPassword() {
        String hashed = hasher.hash("Secret-123");
        assertFalse(hasher.matches("Secret-124", hashed));
    }

    @Test
    void matchesRejectsTamperedHash() {
        String hashed = hasher.hash("password");
        String tampered = hashed.substring(0, hashed.length() - 1) + "X";
        assertFalse(hasher.matches("password", tampered));
    }
}
