package com.aionn.sharedkernel.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void exposesDomainErrorCodeAndMessage() {
        DomainException ex = new DomainException("Catalog", "INVALID", "broken");

        assertEquals("Catalog", ex.getDomain());
        assertEquals("INVALID", ex.getErrorCode());
        assertEquals("broken", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void preservesCauseWhenProvided() {
        Throwable cause = new IllegalStateException("root");

        DomainException ex = new DomainException("Identity", "CONFLICT", "failed", cause);

        assertEquals("Identity", ex.getDomain());
        assertEquals("CONFLICT", ex.getErrorCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    void rejectsNullDomain() {
        assertThrows(NullPointerException.class, () -> new DomainException(null, "CODE", "msg"));
    }

    @Test
    void rejectsNullErrorCode() {
        assertThrows(NullPointerException.class, () -> new DomainException("Catalog", null, "msg"));
    }

    @Test
    void rejectsNullDomainWithCauseConstructor() {
        Throwable cause = new RuntimeException();
        assertThrows(NullPointerException.class, () -> new DomainException(null, "CODE", "msg", cause));
    }

    @Test
    void rejectsNullErrorCodeWithCauseConstructor() {
        Throwable cause = new RuntimeException();
        assertThrows(NullPointerException.class, () -> new DomainException("Catalog", null, "msg", cause));
    }
}
