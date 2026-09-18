package com.aionn.ucp.adapter.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpMessage(
        String type,
        String code,
        String content,
        String severity,
        String path,
        @JsonProperty("content_type") String contentType) {

    public static final String SEVERITY_RECOVERABLE = "recoverable";
    public static final String SEVERITY_REQUIRES_BUYER_INPUT = "requires_buyer_input";
    public static final String SEVERITY_REQUIRES_BUYER_REVIEW = "requires_buyer_review";
    public static final String SEVERITY_UNRECOVERABLE = "unrecoverable";

    public static UcpMessage error(String code, String content, String severity) {
        return new UcpMessage("error", code, content, severity, null, "plain");
    }

    public static UcpMessage error(String code, String content, String severity, String path) {
        return new UcpMessage("error", code, content, severity, path, "plain");
    }
}
