package com.aionn.payment.infrastructure.provider.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VnpayPropertiesTest {

    @Test
    void shouldCreateWithAllFields() {
        VnpayProperties p = new VnpayProperties(
                "TMN", "SECRET", "https://pay.url", "http://return",
                "http://fe/return", "https://api.url", "2.1.0", "pay", "VND", "vn");

        assertEquals("TMN", p.tmnCode());
        assertEquals("SECRET", p.hashSecret());
        assertEquals("https://pay.url", p.payUrl());
        assertEquals("http://return", p.returnUrl());
        assertEquals("http://fe/return", p.frontendReturnUrl());
        assertEquals("https://api.url", p.apiUrl());
        assertEquals("2.1.0", p.version());
        assertEquals("pay", p.command());
        assertEquals("VND", p.currCode());
        assertEquals("vn", p.locale());
    }
}
