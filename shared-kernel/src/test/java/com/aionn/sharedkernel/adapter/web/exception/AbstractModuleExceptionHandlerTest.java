package com.aionn.sharedkernel.adapter.web.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.common.exception.DomainException;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class AbstractModuleExceptionHandlerTest {

        private static final String DOMAIN = "sample";

        static final class CustomDefaultHandler extends AbstractModuleExceptionHandler {

                CustomDefaultHandler() {
                        setDefaultStatus(HttpStatus.I_AM_A_TEAPOT);
                        registerErrors(HttpStatus.BAD_REQUEST, "X_1");
                }

                ResponseEntity<ApiResponse<Map<String, Object>>> handle(DomainException ex) {
                        return handleException(ex);
                }

                HttpStatus statusFor(String errorCode) {
                        return resolveStatus(errorCode);
                }
        }

        @Example
        void example_r10_2_unknownErrorCode_returns422ApiResponseError() {
                SampleModuleExceptionHandler handler = new SampleModuleExceptionHandler();
                DomainException ex = new DomainException(DOMAIN, "SAMPLE_UNKNOWN", "unknown error code");

                ResponseEntity<ApiResponse<Map<String, Object>>> response = handler.handleException(ex);

                assertEquals(422, response.getStatusCode().value(),
                                "unknown errorCode must fall back to 422 UNPROCESSABLE_ENTITY");
                assertEquals("422", response.getBody().statusCode(),
                                "ApiResponse.error statusCode must be \"422\"");
        }

        @Example
        void example_r10_2_unregisteredErrorCode_returns422ApiResponseError() {
                SampleModuleExceptionHandler handler = new SampleModuleExceptionHandler();
                DomainException ex = new DomainException(DOMAIN, "SAMPLE_999", "unregistered error code");

                ResponseEntity<ApiResponse<Map<String, Object>>> response = handler.handleException(ex);

                assertEquals(422, response.getStatusCode().value(),
                                "unregistered errorCode must fall back to 422 UNPROCESSABLE_ENTITY");
                assertEquals("422", response.getBody().statusCode(),
                                "ApiResponse.error statusCode must be \"422\"");
        }

        @Example
        void example_r10_3_resolveStatusThreeCases() {
                SampleModuleExceptionHandler handler = new SampleModuleExceptionHandler();

                assertEquals(HttpStatus.BAD_REQUEST, handler.statusFor("SAMPLE_001"),
                                "registered SAMPLE_001 must map to BAD_REQUEST");
                assertEquals(HttpStatus.NOT_FOUND, handler.statusFor("SAMPLE_404"),
                                "registered SAMPLE_404 must map to NOT_FOUND");

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, handler.statusFor("SAMPLE_999"),
                                "unregistered errorCode must map to default 422");

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, handler.statusFor(null),
                                "null errorCode must map to default 422");
        }

        @Example
        void example_r10_6_setDefaultStatusOverridesFallback() {
                CustomDefaultHandler handler = new CustomDefaultHandler();

                assertEquals(HttpStatus.BAD_REQUEST, handler.statusFor("X_1"),
                                "registered code must keep its registered status");

                assertEquals(HttpStatus.I_AM_A_TEAPOT, handler.statusFor(null),
                                "null errorCode must use the custom default status");
                assertEquals(HttpStatus.I_AM_A_TEAPOT, handler.statusFor("UNKNOWN_CODE"),
                                "unregistered errorCode must use the custom default status");

                ResponseEntity<ApiResponse<Map<String, Object>>> unknownResponse = handler
                                .handle(new DomainException(DOMAIN, "UNKNOWN_CODE", "unknown code"));
                assertEquals(HttpStatus.I_AM_A_TEAPOT.value(), unknownResponse.getStatusCode().value(),
                                "handleException with an unknown code must use the custom default status");
                assertEquals(String.valueOf(HttpStatus.I_AM_A_TEAPOT.value()), unknownResponse.getBody().statusCode(),
                                "ApiResponse.error statusCode must reflect the custom default status");
        }

        @Property(tries = 100)
        void property27_registeredCodeMapsToRegisteredStatus(@ForAll("registeredCodes") String errorCode) {
                SampleModuleExceptionHandler handler = new SampleModuleExceptionHandler();

                HttpStatus expected = "SAMPLE_001".equals(errorCode)
                                ? HttpStatus.BAD_REQUEST
                                : HttpStatus.NOT_FOUND;

                assertEquals(expected, handler.statusFor(errorCode),
                                "registered errorCode must resolve to its registered status");
        }

        @Property(tries = 100)
        void property27_unregisteredCodeFallsBackToDefault(@ForAll String errorCode) {
                Assume.that(!"SAMPLE_001".equals(errorCode) && !"SAMPLE_404".equals(errorCode));

                SampleModuleExceptionHandler handler = new SampleModuleExceptionHandler();

                assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, handler.statusFor(errorCode),
                                "unregistered errorCode must fall back to default 422");
                assertEquals(422, handler.statusFor(errorCode).value(),
                                "default fallback status code must be 422");
        }

        @Provide
        Arbitrary<String> registeredCodes() {
                return Arbitraries.of("SAMPLE_001", "SAMPLE_404");
        }

        @Example
        void example_moduleExceptionHandlerMapsFrameworkExceptions() throws Exception {
                SampleModuleExceptionHandler handler = new SampleModuleExceptionHandler();
                var parameter = new org.springframework.core.MethodParameter(
                                SampleController.class.getDeclaredMethod("submit", String.class), 0);
                var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
                bindingResult.addError(new FieldError("request", "name", "required"));

                var notValid = new MethodArgumentNotValidException(parameter, bindingResult);
                var typeMismatch = new MethodArgumentTypeMismatchException(
                                "abc", Integer.class, "page", parameter, new IllegalArgumentException("bad"));
                var notReadable = new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0]));
                var missingHeader = new MissingRequestHeaderException("X-Trace-Id", parameter);
                var authenticationException = new org.springframework.security.core.AuthenticationException("auth") {
                };

                assertEquals(400, handler.handleMethodArgumentNotValid(notValid).getStatusCode().value());
                assertEquals(401, handler.handleAuthentication(authenticationException).getStatusCode().value());
                assertEquals(403, handler.handleAccessDenied(new org.springframework.security.access.AccessDeniedException("denied"))
                                .getStatusCode().value());
                assertEquals(400, handler.handleTypeMismatch(typeMismatch).getStatusCode().value());
                assertEquals(400, handler.handleNotReadable(notReadable).getStatusCode().value());
                assertEquals(400, handler.handleMissingHeader(missingHeader).getStatusCode().value());
        }

        static class SampleController {
                void submit(String body) {
                        // Empty on purpose; only reflective method metadata is needed in this test.
                }
        }
}
