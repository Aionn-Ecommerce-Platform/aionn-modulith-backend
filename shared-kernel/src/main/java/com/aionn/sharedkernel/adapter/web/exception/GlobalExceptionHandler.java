package com.aionn.sharedkernel.adapter.web.exception;

import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotFound(NotFoundException ex) {
		log.debug("Not found: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(ValidationException ex) {
		log.debug("Validation failed: {}", ex.getMessage());
		Map<String, String> fieldMap = null;
		if (!ex.getFieldErrors().isEmpty()) {
			fieldMap = ex.getFieldErrors().stream()
					.collect(Collectors.toMap(
							ValidationException.FieldError::field,
							ValidationException.FieldError::message,
							(a, b) -> a + "; " + b));
		}
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, fieldMap);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleConflict(ConflictException ex) {
		log.debug("Conflict: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.CONFLICT, ex);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleUnauthorized(UnauthorizedException ex) {
		log.debug("Unauthorized: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleForbidden(ForbiddenException ex) {
		log.debug("Forbidden: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.FORBIDDEN, ex);
	}

	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleDomain(DomainException ex) {
		log.warn("Domain exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
		return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgument(IllegalArgumentException ex) {
		log.debug("Illegal argument: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
				"ILLEGAL_ARGUMENT", null, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex) {
		Map<String, String> fieldMap = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						fe -> fe.getField(),
						fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
						(a, b) -> a + "; " + b));

		log.debug("Request validation failed: {} field error(s)", fieldMap.size());
		return buildErrorResponse(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				"VALIDATION_FAILED",
				"Request",
				fieldMap);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleTypeMismatch(
			MethodArgumentTypeMismatchException ex) {
		String message = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());
		log.debug("Type mismatch: {}", message);
		return buildErrorResponse(HttpStatus.BAD_REQUEST, message, "INVALID_PARAMETER", null, null);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotReadable(HttpMessageNotReadableException ex) {
		log.debug("Malformed request body: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.BAD_REQUEST,
				"Malformed JSON request body", "MALFORMED_BODY", null, null);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleMissingHeader(MissingRequestHeaderException ex) {
		log.debug("Missing header: {}", ex.getHeaderName());
		return buildErrorResponse(HttpStatus.BAD_REQUEST,
				"Missing required header: " + ex.getHeaderName(),
				"MISSING_HEADER", null, null);
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleNoHandler(NoHandlerFoundException ex) {
		log.debug("No handler: {} {}", ex.getHttpMethod(), ex.getRequestURL());
		return buildErrorResponse(HttpStatus.NOT_FOUND,
				"Endpoint not found: " + ex.getRequestURL(), "ENDPOINT_NOT_FOUND", null, null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthentication(AuthenticationException ex) {
		log.debug("Authentication failure: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required",
				"UNAUTHORIZED", null, null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex) {
		log.debug("Access denied: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied",
				"FORBIDDEN", null, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return buildErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred",
				"INTERNAL_ERROR",
				null,
				null);
	}

	protected ResponseEntity<ApiResponse<Map<String, Object>>> buildErrorResponse(
			HttpStatus status,
			DomainException ex) {
		return buildErrorResponse(status, ex, null);
	}

	protected ResponseEntity<ApiResponse<Map<String, Object>>> buildErrorResponse(
			HttpStatus status,
			DomainException ex,
			Map<String, String> fieldErrors) {
		return buildErrorResponse(status, ex.getMessage(), ex.getErrorCode(), ex.getDomain(), fieldErrors);
	}

	protected ResponseEntity<ApiResponse<Map<String, Object>>> buildErrorResponse(
			HttpStatus status,
			String message,
			String errorCode,
			String domain,
			Map<String, String> fieldErrors) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("errorCode", errorCode);
		if (domain != null) {
			body.put("domain", domain);
		}
		if (fieldErrors != null && !fieldErrors.isEmpty()) {
			body.put("fieldErrors", fieldErrors);
		}
		return ResponseEntity.status(status)
				.body(ApiResponse.error(String.valueOf(status.value()), message, body));
	}
}
