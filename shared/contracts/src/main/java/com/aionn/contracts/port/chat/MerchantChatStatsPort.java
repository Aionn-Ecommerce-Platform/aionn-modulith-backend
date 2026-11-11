package com.aionn.contracts.port.chat;

import java.time.Instant;

/** Merchant response statistics used by the public storefront header. */
public interface MerchantChatStatsPort {

    ResponseStats responseStats(String merchantId, Instant since);

    record ResponseStats(long conversations, long respondedConversations, Long medianFirstResponseSeconds) {
    }
}
