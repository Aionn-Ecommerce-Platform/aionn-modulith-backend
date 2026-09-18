package com.aionn.ucp.adapter.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpErrorResponse(
        Metadata ucp,
        List<UcpMessage> messages,
        @JsonProperty("continue_url") String continueUrl) {

    public static final String STATUS_ERROR = "error";

    public static UcpErrorResponse of(String version, String code, String message, String severity) {
        return new UcpErrorResponse(
                new Metadata(version, STATUS_ERROR),
                List.of(UcpMessage.error(code, message, severity)),
                null);
    }

    public static UcpErrorResponse of(String version, String code, String message, String severity, String path) {
        return new UcpErrorResponse(
                new Metadata(version, STATUS_ERROR),
                List.of(UcpMessage.error(code, message, severity, path)),
                null);
    }

    public static UcpErrorResponse of(String version, List<UcpMessage> messages) {
        return new UcpErrorResponse(
                new Metadata(version, STATUS_ERROR),
                messages != null ? List.copyOf(messages) : List.of(),
                null);
    }

    public record Metadata(String version, String status) {
    }
}
