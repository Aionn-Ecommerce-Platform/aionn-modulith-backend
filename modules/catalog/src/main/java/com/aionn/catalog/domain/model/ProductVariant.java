package com.aionn.catalog.domain.model;

import com.aionn.sharedkernel.domain.vo.Money;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ProductVariant {

    /**
     * Parcel characteristics for a sellable unit, used for real carrier fee
     * quotes. {@code weightGram} is mandatory at publish time; the L×W×H box is
     * optional but must be complete when provided.
     */
    public record ParcelDimensions(Integer weightGram, BigDecimal lengthCm, BigDecimal widthCm,
            BigDecimal heightCm) {
        public boolean hasWeight() {
            return weightGram != null && weightGram > 0;
        }

        public boolean hasBox() {
            return lengthCm != null && lengthCm.signum() > 0
                    && widthCm != null && widthCm.signum() > 0
                    && heightCm != null && heightCm.signum() > 0;
        }

        /** Box completeness: all three sides set together or none at all. */
        public boolean isBoxComplete() {
            boolean any = lengthCm != null || widthCm != null || heightCm != null;
            return !any || hasBox();
        }
    }

    private final String skuId;
    private final Map<String, String> attributeValues;
    private Money price;
    private Money originalPrice;
    private ParcelDimensions parcel;

    public ProductVariant(String skuId, Map<String, String> attributeValues, Money price) {
        this(skuId, attributeValues, price, null, null);
    }

    public ProductVariant(String skuId, Map<String, String> attributeValues, Money price,
            Money originalPrice) {
        this(skuId, attributeValues, price, originalPrice, null);
    }

    public ProductVariant(String skuId, Map<String, String> attributeValues, Money price,
            Money originalPrice, ParcelDimensions parcel) {
        this.skuId = skuId;
        this.attributeValues = attributeValues == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(attributeValues);
        this.price = price;
        this.originalPrice = originalPrice;
        this.parcel = parcel;
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

    public ParcelDimensions parcel() {
        return parcel;
    }

    public void setParcel(ParcelDimensions parcel) {
        this.parcel = parcel;
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
