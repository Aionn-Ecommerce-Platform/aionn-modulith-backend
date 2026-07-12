package com.aionn.sharedkernel.adapter.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		String statusCode,
		String message,
		T data,
		Instant timestamp,
		Object paging) {

	private static final String OK = String.valueOf(HttpStatus.OK.value());
	private static final String CREATED = String.valueOf(HttpStatus.CREATED.value());

	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(OK, message, data, Instant.now(Clock.systemUTC()), null);
	}

	public static ApiResponse<Void> success(String message) {
		return new ApiResponse<>(OK, message, null, Instant.now(Clock.systemUTC()), null);
	}

	public static <T> ApiResponse<T> created(String message, T data) {
		return new ApiResponse<>(CREATED, message, data, Instant.now(Clock.systemUTC()), null);
	}

	public static <T> ResponseEntity<ApiResponse<T>> createdResponse(String message, T data) {
		return ResponseEntity.status(HttpStatus.CREATED).body(created(message, data));
	}

	public static <T> ApiResponse<T> successWithPaging(T data, Object paging, String message) {
		return new ApiResponse<>(OK, message, data, Instant.now(Clock.systemUTC()), paging);
	}

	public static <T> ApiResponse<T> error(String statusCode, String message) {
		return new ApiResponse<>(statusCode, message, null, Instant.now(Clock.systemUTC()), null);
	}

	public static <T> ApiResponse<T> error(HttpStatus status, String message) {
		return new ApiResponse<>(String.valueOf(status.value()), message, null, Instant.now(Clock.systemUTC()), null);
	}

	public static <T> ApiResponse<T> error(String statusCode, String message, T data) {
		return new ApiResponse<>(statusCode, message, data, Instant.now(Clock.systemUTC()), null);
	}
}
