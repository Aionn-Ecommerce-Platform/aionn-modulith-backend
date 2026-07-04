package com.aionn.identity.adapter.rest.support.response;

import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class NoStoreResponseFactory {

    public <T> ResponseEntity<ApiResponse<T>> ok(ApiResponse<T> body) {
        return ok(body, null);
    }

    public <T> ResponseEntity<ApiResponse<T>> ok(ApiResponse<T> body, HttpHeaders extraHeaders) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate().cachePrivate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0");
        if (extraHeaders != null && !extraHeaders.isEmpty()) {
            builder.headers(headers -> headers.addAll(extraHeaders));
        }
        return builder.body(body);
    }
}
