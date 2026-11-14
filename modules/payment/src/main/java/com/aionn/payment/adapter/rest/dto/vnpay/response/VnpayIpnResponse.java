package com.aionn.payment.adapter.rest.dto.vnpay.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VnpayIpnResponse(
        @JsonProperty("RspCode") String responseCode,
        @JsonProperty("Message") String message) {
}
