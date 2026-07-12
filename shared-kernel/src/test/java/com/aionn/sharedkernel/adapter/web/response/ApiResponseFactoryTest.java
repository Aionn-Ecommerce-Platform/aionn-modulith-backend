package com.aionn.sharedkernel.adapter.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiResponseFactoryTest {

    @Test
    void successWithDataUses200AndCarriesPayload() {
        ApiResponse<String> response = ApiResponse.success("payload", "ok");

        assertEquals("200", response.statusCode());
        assertEquals("ok", response.message());
        assertEquals("payload", response.data());
        assertNull(response.paging());
        assertNotNull(response.timestamp());
    }

    @Test
    void successMessageOnlyHasNoData() {
        ApiResponse<Void> response = ApiResponse.success("done");

        assertEquals("200", response.statusCode());
        assertEquals("done", response.message());
        assertNull(response.data());
    }

    @Test
    void createdUses201() {
        ApiResponse<String> response = ApiResponse.created("created", "id-1");

        assertEquals("201", response.statusCode());
        assertEquals("id-1", response.data());
    }

    @Test
    void createdResponseWraps201Entity() {
        ResponseEntity<ApiResponse<String>> entity = ApiResponse.createdResponse("created", "id-1");

        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals("201", entity.getBody().statusCode());
        assertEquals("id-1", entity.getBody().data());
    }

    @Test
    void successWithPagingCarriesPagingObject() {
        Object paging = new Object();

        ApiResponse<String> response = ApiResponse.successWithPaging("data", paging, "ok");

        assertEquals("200", response.statusCode());
        assertEquals(paging, response.paging());
    }

    @Test
    void errorWithStringStatusCode() {
        ApiResponse<Void> response = ApiResponse.error("409", "conflict");

        assertEquals("409", response.statusCode());
        assertEquals("conflict", response.message());
        assertNull(response.data());
    }

    @Test
    void errorWithHttpStatusResolvesNumericCode() {
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.NOT_FOUND, "missing");

        assertEquals("404", response.statusCode());
        assertEquals("missing", response.message());
    }

    @Test
    void errorWithDataKeepsPayload() {
        ApiResponse<String> response = ApiResponse.error("422", "invalid", "details");

        assertEquals("422", response.statusCode());
        assertEquals("details", response.data());
    }
}
