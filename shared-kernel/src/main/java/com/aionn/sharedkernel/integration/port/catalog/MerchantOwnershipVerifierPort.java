package com.aionn.sharedkernel.integration.port.catalog;

/**
 * Outbound port for verifying that a given user owns a given merchant.
 *
 * <p>
 * Used by edge layer (argument resolvers) to authorize requests carrying an
 * {@code X-Merchant-Id} header — a user can only operate on merchants they
 * actually own. Implementation lives in the Catalog module which holds the
 * Merchant aggregate.
 * </p>
 *
 * <p>
 * Synchronous because it must block the request before the controller method
 * runs.
 * </p>
 */
public interface MerchantOwnershipVerifierPort {

    /**
     * Verify the owner relationship.
     *
     * @return true if {@code ownerId} owns {@code merchantId}
     */
    boolean isOwnedBy(String merchantId, String ownerId);
}
