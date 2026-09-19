package com.aionn.ucp.adapter.rest.dto.checkout;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpLinkResponse(
        @JsonProperty("type") String type,
        @JsonProperty("url") String url,
        @JsonProperty("title") String title) {

    public static UcpLinkResponse termsOfService(String baseUrl) {
        return new UcpLinkResponse("terms_of_service", baseUrl + "/terms", "Terms of Service");
    }

    public static UcpLinkResponse privacyPolicy(String baseUrl) {
        return new UcpLinkResponse("privacy_policy", baseUrl + "/privacy", "Privacy Policy");
    }
}
