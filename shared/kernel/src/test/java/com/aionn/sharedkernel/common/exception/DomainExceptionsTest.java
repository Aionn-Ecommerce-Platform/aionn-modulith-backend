package com.aionn.sharedkernel.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionsTest {

    @Test
    void notFoundCarriesTheResourceAndId() {
        NotFoundException exception = new NotFoundException("Product", "p-1");

        assertThat(exception.getDomain()).isEqualTo("Product");
        assertThat(exception.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(exception).hasMessage("Product with id 'p-1' not found");
    }

    @Test
    void notFoundSupportsACustomMessage() {
        NotFoundException exception = new NotFoundException("Product", "p-1", "custom");

        assertThat(exception.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(exception).hasMessage("custom");
    }

    @Test
    void forbiddenDescribesTheAction() {
        ForbiddenException exception = new ForbiddenException("delete product");

        assertThat(exception.getDomain()).isEqualTo("Auth");
        assertThat(exception.getErrorCode()).isEqualTo("FORBIDDEN");
        assertThat(exception).hasMessageContaining("delete product");
    }

    @Test
    void forbiddenCanNameTheDomain() {
        ForbiddenException exception = new ForbiddenException("Catalog", "delete product");

        assertThat(exception.getDomain()).isEqualTo("Catalog");
        assertThat(exception.getErrorCode()).isEqualTo("FORBIDDEN");
        assertThat(exception).hasMessageContaining("delete product").hasMessageContaining("Catalog");
    }
}
