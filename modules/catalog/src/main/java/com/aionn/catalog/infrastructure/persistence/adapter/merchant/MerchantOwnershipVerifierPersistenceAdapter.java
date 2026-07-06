package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.sharedkernel.integration.port.catalog.MerchantOwnershipVerifierPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * In-memory adapter that verifies merchant ownership using the Catalog module's
 * MerchantRepository. When the Catalog module is split into a microservice,
 * replace this with a gRPC/HTTP client.
 */
@Component
@RequiredArgsConstructor
public class MerchantOwnershipVerifierPersistenceAdapter implements MerchantOwnershipVerifierPort {

    private final MerchantPersistencePort merchantRepository;

    @Override
    public boolean isOwnedBy(String merchantId, String ownerId) {
        if (merchantId == null || merchantId.isBlank() || ownerId == null || ownerId.isBlank()) {
            return false;
        }
        return merchantRepository.findById(merchantId)
                .map(merchant -> ownerId.equals(merchant.getOwnerId()))
                .orElse(false);
    }
}
