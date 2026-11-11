package com.aionn.shipping.application.policy;

public interface ShippingDefaultsPolicy {

    String defaultCurrency();

    int defaultBaseWeightGram();

    int defaultStepWeightGram();
}
