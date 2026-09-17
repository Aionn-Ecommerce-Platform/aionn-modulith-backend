package com.aionn.ucp.adapter.rest.advice;

import com.aionn.ucp.adapter.rest.dto.UcpErrorResponse;
import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice(basePackages = "com.aionn.ucp.adapter.rest")
public class UcpControllerAdvice {

        private static final Logger log = LoggerFactory.getLogger(UcpControllerAdvice.class);

        private final String version;

        public UcpControllerAdvice() {
                this(null);
        }

        @Autowired
        public UcpControllerAdvice(@Autowired(required = false) UcpProperties properties) {
                this.version = properties != null ? properties.version() : "2026-08-25";
        }

        @ExceptionHandler(UcpProtocolException.class)
        public ResponseEntity<UcpErrorResponse> handleUcpProtocolException(UcpProtocolException ex) {
                log.warn("UCP protocol exception: code={}, severity={}, status={}",
                                ex.getErrorCode(), ex.getSeverity(), ex.getStatusCode());

                UcpErrorResponse response = UcpErrorResponse.of(
                                version,
                                ex.getErrorCode(),
                                ex.getMessage(),
                                ex.getSeverity(),
                                ex.getPath());

                return ResponseEntity
                                .status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<UcpErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
                log.warn("UCP validation failed: {} errors", ex.getBindingResult().getErrorCount());

                List<UcpMessage> messages = new ArrayList<>();
                for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
                        String path = "$." + fieldError.getField();
                        String content = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage()
                                        : "Invalid field value";
                        messages.add(UcpMessage.error("invalid_request", content, UcpMessage.SEVERITY_UNRECOVERABLE,
                                        path));
                }

                if (messages.isEmpty()) {
                        messages.add(UcpMessage.error("invalid_request", "Request validation failed",
                                        UcpMessage.SEVERITY_UNRECOVERABLE));
                }

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(UcpErrorResponse.of(version, messages));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<UcpErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
                log.warn("UCP malformed request body: {}", ex.getMessage());

                UcpErrorResponse response = UcpErrorResponse.of(
                                version,
                                "invalid_request",
                                "Malformed or unreadable JSON payload",
                                UcpMessage.SEVERITY_UNRECOVERABLE);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(response);
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<UcpErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
                log.warn("UCP unsupported media type: {}", ex.getContentType());

                UcpErrorResponse response = UcpErrorResponse.of(
                                version,
                                "unsupported_media_type",
                                "Content-Type must be application/json",
                                UcpMessage.SEVERITY_UNRECOVERABLE);

                return ResponseEntity
                                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(response);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<UcpErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
                log.warn("UCP HTTP method not supported: {}", ex.getMethod());

                UcpErrorResponse response = UcpErrorResponse.of(
                                version,
                                "method_not_allowed",
                                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                                UcpMessage.SEVERITY_UNRECOVERABLE);

                return ResponseEntity
                                .status(HttpStatus.METHOD_NOT_ALLOWED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(response);
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<UcpErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
                log.warn("UCP resource not found: {}", ex.getResourcePath());

                UcpErrorResponse response = UcpErrorResponse.of(
                                version,
                                "not_found",
                                "The requested UCP resource was not found",
                                UcpMessage.SEVERITY_UNRECOVERABLE);

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<UcpErrorResponse> handleGenericException(Exception ex) {
                log.error("Unhandled internal error during UCP request processing", ex);

                // Never leak persistence models, SQL errors or internal stack traces to the
                // caller
                UcpErrorResponse response = UcpErrorResponse.of(
                                version,
                                "internal_error",
                                "An unexpected internal error occurred while processing the UCP operation",
                                UcpMessage.SEVERITY_UNRECOVERABLE);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(response);
        }
}
