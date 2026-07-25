package com.aionn.shipping.infrastructure.carrier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shipping.carrier.ghn")
public record GhnProperties(
        String baseUrl,
        String token,
        String shopId,
        Integer fromDistrictId,
        String fromWardCode,
        Integer serviceId,
        Integer serviceTypeId,
        Integer paymentTypeId,
        String requiredNote,
        String labelPrintUrl,
        String webhookSecret) {

    public GhnProperties {
        if (serviceTypeId == null) {
            serviceTypeId = 2;
        }
        if (paymentTypeId == null) {
            paymentTypeId = 2;
        }
        if (requiredNote == null || requiredNote.isBlank()) {
            requiredNote = "KHONGCHOXEMHANG";
        }
        if (labelPrintUrl == null || labelPrintUrl.isBlank()) {
            labelPrintUrl = "https://dev-online-gateway.ghn.vn/a5/public-api/printA5";
        }
    }
}
