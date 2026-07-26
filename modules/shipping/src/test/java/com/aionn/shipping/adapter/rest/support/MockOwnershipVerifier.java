package com.aionn.shipping.adapter.rest.support;

import com.aionn.sharedkernel.integration.port.catalog.MerchantOwnershipVerifierPort;

/**
 * Test-only ownership verifier used to wire up
 * {@code CurrentMerchantIdArgumentResolver}. Default is permissive so tests
 * can focus on the business path without standing up a full security context.
 */
public class MockOwnershipVerifier implements MerchantOwnershipVerifierPort {

    private boolean allow = true;

    public void allow(boolean allow) {
        this.allow = allow;
    }

    @Override
    public boolean isOwnedBy(String merchantId, String ownerId) {
        return allow && merchantId != null && !merchantId.isBlank()
                && ownerId != null && !ownerId.isBlank();
    }
}
