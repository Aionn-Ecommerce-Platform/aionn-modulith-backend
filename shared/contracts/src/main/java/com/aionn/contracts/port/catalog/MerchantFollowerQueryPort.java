package com.aionn.contracts.port.catalog;

import java.util.List;

/** Read-only follower lookup used by notification fan-out. */
public interface MerchantFollowerQueryPort {

    List<String> findFollowerIds(String merchantId);

    long countFollowers(String merchantId);

    boolean isFollowing(String merchantId, String userId);
}
