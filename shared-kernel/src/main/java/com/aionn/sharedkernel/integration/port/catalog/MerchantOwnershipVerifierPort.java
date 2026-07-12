package com.aionn.sharedkernel.integration.port.catalog;

// Outbound port for verifying that a given user owns a given merchant.
// Used by the edge layer (argument resolvers) to authorize requests carrying an
// X-Merchant-Id header — a user can only operate on merchants they actually own.
// Implementation lives in the Catalog module which holds the Merchant aggregate.
// Synchronous because it must block the request before the controller method runs.
public interface MerchantOwnershipVerifierPort {

    boolean isOwnedBy(String merchantId, String ownerId);
}
