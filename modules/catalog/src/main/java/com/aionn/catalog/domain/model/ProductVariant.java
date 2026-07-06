package com.aionn.catalog.domain.model;

import com.aionn.sharedkernel.domain.vo.Money;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ProductVariant {

    private final String skuId;
    private final Map<String, String> attributeValues;
    private Money price;
    private Money originalPrice;

    public ProductVariant(String skuId, Map<String, String> attributeValues, Money price) {
        this(skuId, attributeValues, price, null);
    }

    public ProductVariant(String skuId, Map<String, String> attributeValues, Money price, Money originalPrice) {
        this.skuId = skuId;
        this.attributeValues = attributeValues == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(attributeValues);
        this.price = price;
        this.originalPrice = originalPrice;
    }

    public String skuId() {
        return skuId;
    }

    public Map<String, String> attributeValues() {
        return Map.copyOf(attributeValues);
    }

    public Money price() {
        return price;
    }

    public void setPrice(Money price) {
        this.price = Objects.requireNonNull(price, "price");
    }

    public Money originalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Money originalPrice) {
        this.originalPrice = originalPrice;
    }

    boolean matches(Map<String, String> other) {
        if (other == null || other.size() != attributeValues.size()) {
            return false;
        }
        for (var entry : attributeValues.entrySet()) {
            if (!Objects.equals(entry.getValue(), other.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
