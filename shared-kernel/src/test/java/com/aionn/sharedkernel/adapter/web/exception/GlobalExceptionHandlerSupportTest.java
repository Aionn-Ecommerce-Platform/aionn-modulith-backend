package com.aionn.sharedkernel.adapter.web.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionn.sharedkernel.common.exception.ConflictException;
import com.aionn.sharedkernel.common.exception.DomainException;
import com.aionn.sharedkernel.common.exception.ForbiddenException;
import com.aionn.sharedkernel.common.exception.NotFoundException;
import com.aionn.sharedkernel.common.exception.UnauthorizedException;
import com.aionn.sharedkernel.common.exception.ValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

class GlobalExceptionHandlerSupportTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void globalExceptionHandlerMapsCoreExceptionFamilies() {
        assertEquals(404, handler.handleNotFound(new NotFoundException("Product", "p-1")).getStatusCode().value());
        assertEquals(409, handler.handleConflict(new ConflictException("Catalog", "CONFLICT", "dup")).getStatusCode().value());
        assertEquals(401, handler.handleUnauthorized(new UnauthorizedException()).getStatusCode().value());
        assertEquals(403, handler.handleForbidden(new ForbiddenException("delete")).getStatusCode().value());
        assertEquals(422, handler.handleDomain(new DomainException("Catalog", "INVALID", "broken")).getStatusCode().value());
        assertEquals(400, handler.handleIllegalArgument(new IllegalArgumentException("bad")).getStatusCode().value());

        ValidationException validationException = new ValidationException("Catalog",
                List.of(new ValidationException.FieldError("name", "required")));
        var validationResponse = handler.handleValidation(validationException);
        assertEquals(400, validationResponse.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) validationResponse.getBody().data().get("fieldErrors");
        assertEquals("required", fieldErrors.get("name"));

        var unexpected = handler.handleUnexpected(new RuntimeException("boom"));
        assertEquals(500, unexpected.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", unexpected.getBody().data().get("errorCode"));
    }

    @Test
    void globalExceptionHandlerMapsSpringWebExceptions() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new MutablePropertyValues(), "request");
        bindingResult.addError(new FieldError("request", "name", "required"));
        MethodParameter parameter = new MethodParameter(
                SampleController.class.getDeclaredMethod("submit", String.class), 0);
        MethodArgumentNotValidException notValid = new MethodArgumentNotValidException(parameter, bindingResult);
        var notValidResponse = handler.handleMethodArgumentNotValid(notValid);
        assertEquals(400, notValidResponse.getStatusCode().value());

        MethodArgumentTypeMismatchException typeMismatch = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", parameter, new IllegalArgumentException("bad"));
        assertEquals(400, handler.handleTypeMismatch(typeMismatch).getStatusCode().value());

        HttpMessageNotReadableException notReadable = new HttpMessageNotReadableException("bad json",
                new MockHttpInputMessage(new byte[0]));
        assertEquals(400, handler.handleNotReadable(notReadable).getStatusCode().value());

        MissingRequestHeaderException missingHeader = new MissingRequestHeaderException("X-Trace-Id", parameter);
        assertEquals(400, handler.handleMissingHeader(missingHeader).getStatusCode().value());

        NoHandlerFoundException noHandler = new NoHandlerFoundException("GET", "/missing", null);
        assertEquals(404, handler.handleNoHandler(noHandler).getStatusCode().value());

        AuthenticationException authenticationException = new AuthenticationException("login required") {
            // Anonymous subclass on purpose to exercise handler against framework base type.
        };
        assertEquals(401, handler.handleAuthentication(authenticationException).getStatusCode().value());
        assertEquals(403, handler.handleAccessDenied(new AccessDeniedException("denied")).getStatusCode().value());
        assertTrue(handler.handleNoHandler(noHandler).getBody().message().contains("/missing"));
    }

    static class SampleController {
        void submit(String body) {
            // Empty on purpose; only reflective method metadata is needed in this test.
        }
    }
}
