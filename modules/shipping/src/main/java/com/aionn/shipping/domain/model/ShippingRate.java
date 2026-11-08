package com.aionn.shipping.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.shipping.domain.event.ShipmentEvents;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Getter
public class ShippingRate extends AggregateRoot {

    private final String rateId;
    private final String zoneCode;
    private BigDecimal baseFee;
    private String currency;
    private String condition;
    private int baseWeightGram;
    private int stepWeightGram;
    private BigDecimal stepFee;
    private BigDecimal maxFee;
    private BigDecimal freeShippingThreshold;
    private final Instant createdAt;
    private Instant updatedAt;

    public ShippingRate(String rateId, String zoneCode, BigDecimal baseFee, String currency,
            String condition, Instant createdAt, Instant updatedAt) {
        this(rateId, zoneCode, baseFee, currency, condition, 1000, 500, BigDecimal.ZERO, null, null,
                createdAt, updatedAt);
    }

    public ShippingRate(String rateId, String zoneCode, BigDecimal baseFee, String currency,
            String condition, int baseWeightGram, int stepWeightGram, BigDecimal stepFee,
            BigDecimal maxFee, BigDecimal freeShippingThreshold,
            Instant createdAt, Instant updatedAt) {
        this.rateId = rateId;
        this.zoneCode = zoneCode;
        this.baseFee = baseFee;
        this.currency = currency;
        this.condition = condition;
        this.baseWeightGram = baseWeightGram;
        this.stepWeightGram = stepWeightGram;
        this.stepFee = stepFee;
        this.maxFee = maxFee;
        this.freeShippingThreshold = freeShippingThreshold;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ShippingRate configure(
            String rateId, String zoneCode, BigDecimal baseFee, String currency, String condition, Clock clock) {
        return configure(rateId, zoneCode, baseFee, currency, condition, 1000, 500, BigDecimal.ZERO, null, null,
                clock);
    }

    public static ShippingRate configure(
            String rateId, String zoneCode, BigDecimal baseFee, String currency, String condition,
            int baseWeightGram, int stepWeightGram, BigDecimal stepFee, BigDecimal maxFee,
            BigDecimal freeShippingThreshold, Clock clock) {
        Guard.require(zoneCode != null && !zoneCode.isBlank(),
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "zoneCode must not be blank"));
        Guard.require(baseFee != null && baseFee.signum() >= 0,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "baseFee must be >= 0"));
        validateWeightLadder(baseWeightGram, stepWeightGram, stepFee, maxFee, freeShippingThreshold);
        Instant now = clock.instant();
        ShippingRate r = new ShippingRate(rateId, zoneCode, baseFee, currency, condition, baseWeightGram,
                stepWeightGram, stepFee, maxFee, freeShippingThreshold, now, now);
        r.registerEvent(new ShipmentEvents.ShippingRateConfigured(rateId, zoneCode, baseFee, currency, condition,
                now, now));
        return r;
    }

    public void update(BigDecimal baseFee, String condition, Clock clock) {
        if (baseFee != null) {
            Guard.require(baseFee.signum() >= 0,
                    () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "baseFee must be >= 0"));
            this.baseFee = baseFee;
        }
        if (condition != null)
            this.condition = condition;
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new ShipmentEvents.ShippingRateConfigured(rateId, zoneCode, this.baseFee, currency,
                this.condition, now, now));
    }

    /** Null fields keep their current value, mirroring {@link #update}. */
    public void updateWeightLadder(int baseWeightGram, int stepWeightGram, BigDecimal stepFee,
            BigDecimal maxFee, BigDecimal freeShippingThreshold, Clock clock) {
        validateWeightLadder(baseWeightGram, stepWeightGram, stepFee, maxFee, freeShippingThreshold);
        this.baseWeightGram = baseWeightGram;
        this.stepWeightGram = stepWeightGram;
        this.stepFee = stepFee;
        this.maxFee = maxFee;
        this.freeShippingThreshold = freeShippingThreshold;
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new ShipmentEvents.ShippingRateConfigured(rateId, zoneCode, this.baseFee, currency,
                this.condition, now, now));
    }

    /**
     * Base fee covers the first {@code baseWeightGram}; each further (or part) {@code stepWeightGram}
     * adds {@code stepFee}. {@code maxFee} caps it, and a subtotal at or above the freeship threshold
     * waives it.
     */
    public BigDecimal feeFor(int chargeableWeightGram, BigDecimal goodsSubtotal) {
        if (isFreeShipping(goodsSubtotal)) {
            return BigDecimal.ZERO;
        }
        if (stepFee == null || stepFee.signum() == 0 || stepWeightGram <= 0) {
            return baseFee;
        }
        int extraGram = Math.max(0, chargeableWeightGram - baseWeightGram);
        long extraSteps = (extraGram + stepWeightGram - 1L) / stepWeightGram;
        BigDecimal fee = baseFee.add(stepFee.multiply(BigDecimal.valueOf(extraSteps)));
        return maxFee != null && fee.compareTo(maxFee) > 0 ? maxFee : fee;
    }

    public boolean isFreeShipping(BigDecimal goodsSubtotal) {
        return freeShippingThreshold != null && goodsSubtotal != null
                && goodsSubtotal.compareTo(freeShippingThreshold) >= 0;
    }

    private static void validateWeightLadder(int baseWeightGram, int stepWeightGram, BigDecimal stepFee,
            BigDecimal maxFee, BigDecimal freeShippingThreshold) {
        Guard.require(baseWeightGram > 0,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "baseWeightGram must be > 0"));
        Guard.require(stepWeightGram > 0,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "stepWeightGram must be > 0"));
        Guard.require(stepFee == null || stepFee.signum() >= 0,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "stepFee must be >= 0"));
        Guard.require(maxFee == null || maxFee.signum() >= 0,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "maxFee must be >= 0"));
        Guard.require(freeShippingThreshold == null || freeShippingThreshold.signum() >= 0,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                        "freeShippingThreshold must be >= 0"));
    }

    @Override
    protected String aggregateId() {
        return rateId;
    }
}
