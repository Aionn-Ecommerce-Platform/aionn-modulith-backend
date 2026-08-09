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
    private final Instant createdAt;
    private Instant updatedAt;

    public ShippingRate(String rateId, String zoneCode, BigDecimal baseFee, String currency,
            String condition, Instant createdAt, Instant updatedAt) {
        this.rateId = rateId;
        this.zoneCode = zoneCode;
        this.baseFee = baseFee;
        this.currency = currency;
        this.condition = condition;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ShippingRate configure(
            String rateId, String zoneCode, BigDecimal baseFee, String currency, String condition, Clock clock) {
        Guard.require(zoneCode != null && !zoneCode.isBlank(),
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "zoneCode must not be blank"));
        Guard.require(baseFee != null && baseFee.signum() >= 0,
                () -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "baseFee must be >= 0"));
        Instant now = clock.instant();
        ShippingRate r = new ShippingRate(rateId, zoneCode, baseFee, currency, condition, now, now);
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

    @Override
    protected String aggregateId() {
        return rateId;
    }
}
