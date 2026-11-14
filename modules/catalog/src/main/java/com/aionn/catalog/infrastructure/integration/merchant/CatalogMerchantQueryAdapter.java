package com.aionn.catalog.infrastructure.integration.merchant;

import java.math.BigDecimal;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.contracts.port.catalog.MerchantCommandPort;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CatalogMerchantQueryAdapter implements MerchantQueryPort, MerchantCommandPort {

    private final MerchantPersistencePort merchantPersistencePort;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findMerchantIdByOwnerId(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            return Optional.empty();
        }
        return merchantPersistencePort.findByOwnerId(ownerId).map(Merchant::getMerchantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findOwnerIdByMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        return merchantPersistencePort.findById(merchantId).map(Merchant::getOwnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findCommissionRate(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        return merchantPersistencePort.findById(merchantId).map(Merchant::getCommissionRate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StripeConnectInfo> findStripeConnectInfo(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        return merchantPersistencePort.findById(merchantId)
                .filter(m -> m.getStripeAccountId() != null)
                .map(m -> new StripeConnectInfo(
                        m.getStripeAccountId(),
                        m.isStripeChargesEnabled(),
                        m.isStripePayoutsEnabled()));
    }

    @Override
    @Transactional
    public void saveStripeAccountId(String merchantId, String stripeAccountId) {
        Merchant merchant = merchantPersistencePort.findById(merchantId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        merchant.linkStripeAccount(stripeAccountId, clock);
        merchantPersistencePort.save(merchant);
    }

    @Override
    @Transactional
    public void updateStripeCapabilities(String merchantId, boolean chargesEnabled, boolean payoutsEnabled) {
        Merchant merchant = merchantPersistencePort.findById(merchantId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        merchant.updateStripeCapabilities(chargesEnabled, payoutsEnabled, clock);
        merchantPersistencePort.save(merchant);
    }
}
