package com.aionn.sharedkernel.infrastructure.web.idempotency;

public record IdempotencyRecord(
        String state,
        String requestHash,
        StoredHttpResponse response) {

    public static IdempotencyRecord processing(String requestHash) {
        return new IdempotencyRecord("PROCESSING", requestHash, null);
    }

    public static IdempotencyRecord completed(String requestHash, StoredHttpResponse response) {
        return new IdempotencyRecord("COMPLETED", requestHash, response);
    }

    public boolean isProcessing() {
        return "PROCESSING".equals(state);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(state);
    }

    public record StoredHttpResponse(
            int status,
            String contentType,
            String bodyJson) {
    }
}
