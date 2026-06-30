package com.aionn.sharedkernel.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionn.sharedkernel.common.exception.ConflictException;
import com.aionn.sharedkernel.common.exception.DomainException;
import com.aionn.sharedkernel.common.exception.ForbiddenException;
import com.aionn.sharedkernel.common.exception.NotFoundException;
import com.aionn.sharedkernel.common.exception.UnauthorizedException;
import com.aionn.sharedkernel.common.exception.ValidationException;
import com.aionn.sharedkernel.media.CloudinaryAutoConfiguration;
import com.aionn.sharedkernel.media.CloudinaryCredentialsProperties;
import com.aionn.sharedkernel.media.CloudinarySigner;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SharedKernelSupportTest {

    @Test
    void domainAndDerivedExceptionsExposeExpectedFields() {
        DomainException domainException = new DomainException("Catalog", "INVALID", "broken");
        ConflictException conflictException = new ConflictException("Catalog", "CONFLICT", "duplicate");
        ForbiddenException forbiddenDefault = new ForbiddenException("checkout");
        ForbiddenException forbiddenScoped = new ForbiddenException("Catalog", "delete");
        NotFoundException notFoundDefault = new NotFoundException("Product", "p-1");
        NotFoundException notFoundCustom = new NotFoundException("Product", "p-1", "custom");
        UnauthorizedException unauthorizedDefault = new UnauthorizedException();
        UnauthorizedException unauthorizedCustom = new UnauthorizedException("Login first");
        ValidationException validationDefault = new ValidationException("Catalog", "INVALID_NAME", "bad");
        ValidationException validationWithFields = new ValidationException("Catalog",
                List.of(new ValidationException.FieldError("name", "required")));

        assertEquals("Catalog", domainException.getDomain());
        assertEquals("CONFLICT", conflictException.getErrorCode());
        assertTrue(forbiddenDefault.getMessage().contains("checkout"));
        assertTrue(forbiddenScoped.getMessage().contains("Catalog"));
        assertTrue(notFoundDefault.getMessage().contains("p-1"));
        assertEquals("custom", notFoundCustom.getMessage());
        assertEquals("UNAUTHORIZED", unauthorizedDefault.getErrorCode());
        assertEquals("Login first", unauthorizedCustom.getMessage());
        assertTrue(validationDefault.getFieldErrors().isEmpty());
        assertEquals(1, validationWithFields.getFieldErrors().size());
        assertThrows(NullPointerException.class, () -> new DomainException("Catalog", null, "bad"));
        assertThrows(NullPointerException.class, () -> new ValidationException("Catalog",
                List.of(new ValidationException.FieldError(null, "required"))));
    }

    @Test
    void cloudinaryHelpersBuildUploadUrlAndStableSignature() {
        String signature = CloudinarySigner.sign(Map.of("public_id", "sample", "timestamp", "1719705600"), "secret");
        String reorderedSignature = CloudinarySigner.sign(
                Map.of("timestamp", "1719705600", "public_id", "sample"),
                "secret");
        CloudinaryCredentialsProperties properties = new CloudinaryCredentialsProperties(
                "aionn", "key", "secret", "https://api.cloudinary.com/v1_1");

        assertNotNull(new CloudinaryAutoConfiguration());
        assertEquals("https://api.cloudinary.com/v1_1/aionn/image/upload", properties.uploadUrl("image"));
        assertEquals(signature, reorderedSignature);
        assertEquals("https://api.cloudinary.com/v1_1/aionn/video/upload",
                CloudinarySigner.uploadUrl("https://api.cloudinary.com/v1_1", "aionn", "video"));
        assertEquals(40, signature.length());
        assertTrue(signature.matches("[0-9a-f]+"));
    }
}
