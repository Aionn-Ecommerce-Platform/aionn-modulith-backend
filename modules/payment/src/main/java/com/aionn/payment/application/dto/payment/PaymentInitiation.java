package com.aionn.payment.application.dto.payment;

import com.aionn.payment.domain.model.Payment;

public record PaymentInitiation(Payment payment, String redirectUrl) {}
