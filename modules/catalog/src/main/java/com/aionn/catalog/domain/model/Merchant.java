package com.aionn.catalog.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.MerchantStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
public class Merchant extends AggregateRoot {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.0500");

    private final String merchantId;
    private final String ownerId;
    private String name;
    private String logoUrl;
    private String description;
    private String provinceCode;
    private String provinceName;
    private MerchantStatus status;
    private BigDecimal commissionRate;
    private String stripeAccountId;
    private boolean stripeChargesEnabled;
    private boolean stripePayoutsEnabled;
    private final Instant createdAt;
    private Instant updatedAt;

    public Merchant(
            String merchantId,
            String ownerId,
            String name,
            String logoUrl,
            String description,
            String provinceCode,
            String provinceName,
            MerchantStatus status,
            BigDecimal commissionRate,
            String stripeAccountId,
            boolean stripeChargesEnabled,
            boolean stripePayoutsEnabled,
            Instant createdAt,
            Instant updatedAt) {
        this.merchantId = merchantId;
        this.ownerId = ownerId;
        this.name = name;
        this.logoUrl = logoUrl;
        this.description = description;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.status = status;
        this.commissionRate = commissionRate != null ? commissionRate : DEFAULT_COMMISSION_RATE;
        this.stripeAccountId = stripeAccountId;
        this.stripeChargesEnabled = stripeChargesEnabled;
        this.stripePayoutsEnabled = stripePayoutsEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Merchant register(String merchantId, String ownerId, String name, BigDecimal commissionRate) {
        Instant now = Instant.now();
        Merchant merchant = new Merchant(merchantId, ownerId, name, null, null, null, null,
                MerchantStatus.PENDING, commissionRate, null, false, false, now, now);
        merchant.registerEvent(new MerchantEvents.MerchantRegistered(
                merchantId, ownerId, name, MerchantStatus.PENDING.name(), now));
        return merchant;
    }

    public void linkStripeAccount(String stripeAccountId) {
        this.stripeAccountId = stripeAccountId;
        this.updatedAt = Instant.now();
    }

    public void updateStripeCapabilities(boolean chargesEnabled, boolean payoutsEnabled) {
        this.stripeChargesEnabled = chargesEnabled;
        this.stripePayoutsEnabled = payoutsEnabled;
        this.updatedAt = Instant.now();
    }

    public void updateCommissionRate(BigDecimal newRate) {
        Guard.require(newRate != null && newRate.signum() >= 0
                && newRate.compareTo(BigDecimal.ONE) <= 0,
                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                        "commission rate must be between 0 and 1"));
        this.commissionRate = newRate;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String name, String logoUrl, String description,
            String provinceCode, String provinceName) {
        Guard.require(name != null && !name.isBlank(),
                () -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, "name must not be blank"));
        this.name = name.trim();
        this.logoUrl = logoUrl;
        this.description = description;
        boolean provinceChanged = !Objects.equals(this.provinceCode, provinceCode);
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.updatedAt = Instant.now();
        if (this.status == MerchantStatus.PENDING) {
            this.status = MerchantStatus.ACTIVE;
        }
        registerEvent(new MerchantEvents.MerchantProfileUpdated(merchantId, this.name, logoUrl, description,
                provinceCode, provinceName, provinceChanged, updatedAt));
    }

    public void suspend(String adminId, String reason) {
        ensureTransitionAllowed(MerchantStatus.SUSPENDED);
        this.status = MerchantStatus.SUSPENDED;
        this.updatedAt = Instant.now();
        registerEvent(new MerchantEvents.MerchantSuspended(merchantId, reason, adminId, updatedAt, updatedAt));
    }

    public void activate(String adminId, String reason) {
        ensureTransitionAllowed(MerchantStatus.ACTIVE);
        this.status = MerchantStatus.ACTIVE;
        this.updatedAt = Instant.now();
        registerEvent(new MerchantEvents.MerchantActivated(merchantId, adminId, reason, updatedAt, updatedAt));
    }

    public void close(String reason) {
        ensureTransitionAllowed(MerchantStatus.CLOSED);
        this.status = MerchantStatus.CLOSED;
        this.updatedAt = Instant.now();
        registerEvent(new MerchantEvents.MerchantClosed(merchantId, reason, updatedAt, updatedAt));
    }

    private void ensureTransitionAllowed(MerchantStatus next) {
        Guard.require(status.canTransitionTo(next),
                () -> new CatalogException(CatalogErrorCode.MERCHANT_INVALID_TRANSITION,
                        "Cannot transition merchant from " + status + " to " + next));
    }

    @Override
    protected String aggregateId() {
        return merchantId;
    }
}
