package com.aionn.sharedkernel.adapter.web.exception;

import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
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

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleOptimisticLockingFailure(
			OptimisticLockingFailureException ex) {
		log.debug("Concurrent aggregate modification: {}", ex.getMessage());
		return buildErrorResponse(
				HttpStatus.CONFLICT,
				"The resource was modified by another request. Reload it and try again.",
				"CONCURRENT_MODIFICATION",
				null,
				null);
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
		return buildErrorResponse(HttpStatus.UNPROCESSABLE_CONTENT, ex);
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

	/**
	 * Native controller-method validation, available since Spring Framework 6.1.
	 *
	 * <p>Constraints declared on {@code @RequestParam} and {@code @PathVariable} arguments are enforced by
	 * {@code HandlerMethodValidator} without needing a class-level {@code @Validated}, and the failure
	 * arrives as this exception rather than as {@link MethodArgumentNotValidException}, which only covers
	 * {@code @Valid @RequestBody}. Without an explicit mapping it falls through to
	 * {@link #handleUnexpected}, so a caller sending {@code ?limit=0} is told the server broke instead of
	 * that the request was invalid - and the server logs a stack trace at ERROR for ordinary bad input.
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleHandlerMethodValidation(
			HandlerMethodValidationException ex) {
		Map<String, String> fieldMap = new LinkedHashMap<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			String parameter = parameterName(result);
			for (MessageSourceResolvable error : result.getResolvableErrors()) {
				String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
				fieldMap.merge(parameter, message, (existing, added) -> existing + "; " + added);
			}
		}

		log.debug("Request parameter validation failed: {} parameter(s)", fieldMap.size());
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

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleMissingParameter(
			MissingServletRequestParameterException ex) {
		log.debug("Missing parameter: {}", ex.getParameterName());
		return buildErrorResponse(HttpStatus.BAD_REQUEST,
				"Missing required parameter: " + ex.getParameterName(),
				"MISSING_PARAMETER", null, null);
	}

	/**
	 * A request to the right path with the wrong verb.
	 *
	 * <p>Reported as 405 with the verbs the endpoint does accept. Left to {@link #handleUnexpected} it
	 * becomes a 500, which sends whoever is debugging - a client author or an E2E script - looking for a
	 * server fault that does not exist, when the only thing wrong is the method on the request line.
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodNotSupported(
			HttpRequestMethodNotSupportedException ex) {
		String supported = ex.getSupportedMethods() != null
				? String.join(", ", ex.getSupportedMethods())
				: "none";
		String message = "Method %s is not supported; supported: %s".formatted(ex.getMethod(), supported);
		log.debug("Unsupported request method: {}", message);
		var response = buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message, "METHOD_NOT_ALLOWED", null, null);
		return ResponseEntity.status(response.getStatusCode())
				.headers(ex.getHeaders())
				.body(response.getBody());
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

	/**
	 * Names the offending parameter for the {@code fieldErrors} payload.
	 *
	 * <p>{@code getParameterName()} is only populated when a {@code ParameterNameDiscoverer} resolved it,
	 * which needs {@code -parameters} at compile time. Falling back to the positional index keeps the
	 * response honest about which argument failed instead of collapsing every violation onto one key.
	 */
	private static String parameterName(ParameterValidationResult result) {
		MethodParameter parameter = result.getMethodParameter();
		String name = parameter.getParameterName();
		return name != null && !name.isBlank() ? name : "arg" + parameter.getParameterIndex();
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
